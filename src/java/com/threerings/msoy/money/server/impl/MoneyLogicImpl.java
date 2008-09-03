//
// $Id$

package com.threerings.msoy.money.server.impl;

import java.math.BigDecimal;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.io.PersistenceException;

import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.data.UserActionDetails;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.UserActionRepository;

import com.threerings.msoy.item.data.all.CatalogIdent;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;

import com.threerings.msoy.money.data.all.MemberMoney;
import com.threerings.msoy.money.data.all.MoneyHistory;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.PriceQuote;
import com.threerings.msoy.money.data.all.TransactionType;

import com.threerings.msoy.money.server.MoneyConfiguration;
import com.threerings.msoy.money.server.MoneyExchange;
import com.threerings.msoy.money.server.MoneyLogic;
import com.threerings.msoy.money.server.MoneyResult;
import com.threerings.msoy.money.server.NotEnoughMoneyException;
import com.threerings.msoy.money.server.NotSecuredException;

import com.threerings.msoy.money.server.persist.MemberAccountHistoryRecord;
import com.threerings.msoy.money.server.persist.MemberAccountRecord;
import com.threerings.msoy.money.server.persist.MoneyRepository;
import com.threerings.msoy.money.server.persist.PersistentCurrency;
import com.threerings.msoy.money.server.persist.PersistentTransactionType;
import com.threerings.msoy.money.server.persist.RepositoryException;
import com.threerings.msoy.money.server.persist.StaleDataException;

/**
 * Default implementation of the money service.
 *
 * TODO: Transactional support
 *
 * @author Kyle Sampson <kyle@threerings.net>
 * @author Ray Greenwell <ray@threerings.net>
 */
@Singleton
public class MoneyLogicImpl
    implements MoneyLogic
{
    @Inject
    public MoneyLogicImpl (
        final MoneyRepository repo, final EscrowCache escrowCache,
        final MoneyHistoryExpirer expirer, final UserActionRepository userActionRepo,
        final MsoyEventLogger eventLog)
    {
        _repo = repo;
        _escrowCache = escrowCache;
        _userActionRepo = userActionRepo;
        _eventLog = eventLog;
        _expirer = expirer;
    }

    @Retry(exception=StaleDataException.class)
    public MoneyResult awardCoins (
        final int memberId, final int creatorId, final int affiliateId,
        final ItemIdent item, final int amount, final String description, final
        UserAction userAction)
    {
        Preconditions.checkArgument(!MemberName.isGuest(memberId), "Cannot award coins to guests.");
        Preconditions.checkArgument(amount >= 0, "amount is invalid: %d", amount);
        Preconditions.checkArgument(item == null || item.itemId != 0 || item.type != 0,
            "item is invalid: %s", (item == null ? null : item.toString()));

        MemberAccountRecord account = _repo.getAccountById(memberId);
        if (account == null) {
            account = new MemberAccountRecord(memberId);
        }
        final MemberAccountHistoryRecord history = account.awardCoins(amount, item, description);
        _repo.saveAccount(account);
        _repo.addHistory(history);

        // TODO: creator and affiliate

        // TODO: what the fuck is going on here, copy/paste dupe bug?
        // TODO #2: sort out logging the catalogId
        logUserAction(memberId, UserActionDetails.INVALID_ID, userAction, null /*item*/,
            description);
        final UserActionDetails info = logUserAction(memberId, 0, userAction, null /*item*/,
            description);
        logInPanopticon(info, Currency.COINS, amount, account);
        
        return new MoneyResult(account.getMemberMoney(), null, null, 
            history.createMoneyHistory(null), null, null);
    }

    @Retry(exception=StaleDataException.class)
    public MoneyResult buyBars (final int memberId, final int numBars, final String description)
    {
        Preconditions.checkArgument(!MemberName.isGuest(memberId), "Guests cannot buy bars.");
        Preconditions.checkArgument(numBars >= 0, "numBars is invalid: %d", numBars);

        MemberAccountRecord account = _repo.getAccountById(memberId);
        if (account == null) {
            account = new MemberAccountRecord(memberId);
        }
        final MemberAccountHistoryRecord history = account.buyBars(numBars, description);
        _repo.saveAccount(account);
        _repo.addHistory(history);

        logUserAction(memberId, UserActionDetails.INVALID_ID, UserAction.BOUGHT_BARS, null,
            history.getDescription());

        return new MoneyResult(account.getMemberMoney(), null, null, 
            history.createMoneyHistory(null), null, null);
    }

    @Retry(exception=StaleDataException.class)
    public MoneyResult buyItem (
        final MemberRecord buyrec, CatalogIdent item, Currency listedCurrency, int listedAmount,
        Currency buyCurrency, int buyAmount)
        throws NotEnoughMoneyException, NotSecuredException
    {
        Preconditions.checkArgument(item != null && (item.type != 0 || item.catalogId != 0),
            "item is invalid: %s", item.toString());
        Preconditions.checkArgument(
            buyCurrency == Currency.BARS || buyCurrency == Currency.COINS,
            "buyCurrency is invalid: %s", buyCurrency.toString());

        // Get the secured prices for the item.
        final PriceKey key = new PriceKey(buyrec.memberId, item);
        final Escrow escrow = _escrowCache.getEscrow(key);
        if (escrow == null) {
            // TODO: 
            // What we need to do here is also have the sellerId, affiliateId, and description..
            // so that we can secure a price. If the buyAmount is at least the priceQuote,
            // we can go ahead and process the purchase here. Otherwise we need to throw
            // the exception with the new PriceQuote inside it! Why not! Do the fucking thing
            // that will need to be done anyway!
            throw new NotSecuredException(buyrec.memberId, item);
        }
        final PriceQuote quote = escrow.getQuote();

        if (!buyrec.isSupport() && !quote.isSatisfied(buyCurrency, buyAmount)) {
            // TODO: see TODO note above, only here we actually have a quote already secured
            // (And we should either get a new quote, or just leave the one in the cache,
            // but it would be an exploit to extend the lifetime of the quote)
            throw new NotSecuredException(buyrec.memberId, item);
        }

        // Get buyer account and make sure they can afford the item.
        final MemberAccountRecord buyer = _repo.getAccountById(buyrec.memberId);
        if (buyer == null || (!buyrec.isSupport() && !buyer.canAfford(buyCurrency, buyAmount))) {
            int hasAmount = (buyer == null) ? 0 : buyer.getAmount(buyCurrency);
            throw new NotEnoughMoneyException(buyrec.memberId, buyCurrency, buyAmount, hasAmount);
        }

        // TODO: move this until after the purchase... this WHOLE fucking method should
        // accept a Runnable, maybe, to process the purchase
        // Inform the exchange that we've actually made the exchange
        MoneyExchange.processPurchase(quote, buyCurrency);

        int amount = quote.getListedAmount();

        // If the creator is buying their own item, don't give them a payback, and deduct the amount
        // they would have received.
        int creatorId = escrow.getCreatorId();
        boolean buyerIsCreator = (buyrec.memberId == creatorId);
        if (buyerIsCreator) {
            // TODO: hmmmm
            amount -= (int)(0.3 * amount);
        }

        // Get creator.
        MemberAccountRecord creator;
        if (buyerIsCreator) {
            creator = buyer;
        } else {
            creator = _repo.getAccountById(creatorId);
            if (creator == null) {
                creator = new MemberAccountRecord(creatorId);
            }
        }

        // Update the member account
        final MemberAccountHistoryRecord history = buyer.buyItem(
            buyCurrency, amount, escrow.getDescription(), item, buyrec.isSupport());
        _repo.addHistory(history);
        _repo.saveAccount(buyer);
        // TODO: fucking fuck, we want to change this to the CatalogIdent
        UserActionDetails info = logUserAction(buyrec.memberId, UserActionDetails.INVALID_ID,
            UserAction.BOUGHT_ITEM, null /*item*/, escrow.getDescription());
        logInPanopticon(info, buyCurrency, history.getSignedAmount(), buyer);

        // Update the creator account, if they get a payment.
        MemberAccountHistoryRecord creatorHistory;
        if (buyerIsCreator) {
            creatorHistory = history;

        } else {
            creatorHistory = creator.creatorPayout(quote.getListedCurrency(),
                (int)history.getAmount(),
                // TODO: this is wrong, it needs translating
                "Item purchased: " + escrow.getDescription(), item, 0.3f, history.id);
            _repo.addHistory(creatorHistory);
            _repo.saveAccount(creator);
            // TODO: fucking fuck, we want to change this to the CatalogIdent
            info = logUserAction(creatorId, buyrec.memberId, UserAction.RECEIVED_PAYOUT,
                                 null /*item*/, escrow.getDescription());
            logInPanopticon(info, buyCurrency, creatorHistory.getSignedAmount(), creator);
        }

        // TODO: update affiliate with some amount of bling.

        // The item no longer needs to be in the cache.
        _escrowCache.removeEscrow(key);

        final MoneyHistory mh = history.createMoneyHistory(null);
        final MoneyHistory creatorMH = creatorHistory.createMoneyHistory(mh);
        return new MoneyResult(buyer.getMemberMoney(),
            buyerIsCreator ? null : creator.getMemberMoney(),
            null, mh, buyerIsCreator ? null : creatorHistory.createMoneyHistory(mh), null);
    }

    public void deductBling (final int memberId, final double amount)
    {
        // TODO Auto-generated method stub
    }

    public int exchangeBlingForBars (final int memberId, final double blingAmount)
        throws NotEnoughMoneyException
    {
        // TODO Auto-generated method stub
        return 0;
    }

    public BigDecimal getBlingWorth (final int memberId)
    {
        // TODO Auto-generated method stub
        return null;
    }

    protected static EnumSet<PersistentTransactionType> toPersist (
        final EnumSet<TransactionType> transactionTypes)
    {
        if (transactionTypes == null) {
            return EnumSet.allOf(PersistentTransactionType.class);
        } else {
            EnumSet<PersistentTransactionType> persistTransactionTypes =
                EnumSet.noneOf(PersistentTransactionType.class);
            for (TransactionType transactionType : transactionTypes) {
                persistTransactionTypes.add(
                    PersistentTransactionType.fromTransactionType(transactionType));
            }

            return persistTransactionTypes;
        }
    }

    public List<MoneyHistory> getLog (
        final int memberId, final Currency currency, final EnumSet<TransactionType> transactionTypes,
        final int start, final int count, final boolean descending)
    {
        Preconditions.checkArgument(!MemberName.isGuest(memberId),
            "Cannot retrieve money log for guests.");
        Preconditions.checkArgument(start >= 0, "start is invalid: %d", start);
        Preconditions.checkArgument(count > 0, "count is invalid: %d", count);

        final List<MemberAccountHistoryRecord> records = _repo.getHistory(memberId, 
            PersistentCurrency.fromCurrency(currency), toPersist(transactionTypes), start, count, 
            descending);
        
        // Put all records into a map by their ID.  We'll use this map to get a set of history ID's
        // that we currently have.
        final Map<Integer, MoneyHistory> referenceMap = new HashMap<Integer, MoneyHistory>();
        for (final MemberAccountHistoryRecord record : records) {
            referenceMap.put(record.id, record.createMoneyHistory(null));
        }
        
        // Create a set of reference transaction IDs we don't already have.  We'll look these up.
        final Set<Integer> lookupRefIds = new HashSet<Integer>();
        for (final MemberAccountHistoryRecord record : records) {
            if (record.referenceTxId != 0 && !referenceMap.keySet().contains(record.referenceTxId)) {
                lookupRefIds.add(record.referenceTxId);
            }
        }
        if (lookupRefIds.size() > 0) {
            for (final MemberAccountHistoryRecord record : _repo.getHistory(lookupRefIds)) {
                referenceMap.put(record.id, record.createMoneyHistory(null));
            }
        }
        
        // Now create the money histories, using the reference map for the references as necessary.
        final List<MoneyHistory> log = new ArrayList<MoneyHistory>();
        for (final MemberAccountHistoryRecord record : records) {
            log.add(record.createMoneyHistory(record.referenceTxId == 0 ? null : 
                referenceMap.get(record.referenceTxId)));
        }
        
        return log;
    }

    public int getHistoryCount (
        final int memberId, final Currency currency, final EnumSet<TransactionType> transactionTypes)
    {
        return _repo.getHistoryCount(memberId, PersistentCurrency.fromCurrency(currency),
            toPersist(transactionTypes));
    }

    public MoneyConfiguration getMoneyConfiguration ()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public MemberMoney getMoneyFor (final int memberId)
    {
        Preconditions.checkArgument(!MemberName.isGuest(memberId),
            "Cannot retrieve money info for guests.");

        final MemberAccountRecord account = _repo.getAccountById(memberId);
        return account != null ? account.getMemberMoney() : new MemberMoney(memberId);
    }

    // from MoneyLogic
    public PriceQuote securePrice (
        int buyerId, CatalogIdent item, Currency listedCurrency, int listedAmount,
        int sellerId, int affiliateId, String description)
    {
        Preconditions.checkArgument(!MemberName.isGuest(buyerId), "Guests cannot secure prices.");
        Preconditions.checkArgument(!MemberName.isGuest(sellerId), "Creators cannot be guests.");
        Preconditions.checkArgument(item != null && (item.type != 0 || item.catalogId != 0),
            "item is invalid: %s", item.toString());
        Preconditions.checkArgument(listedAmount >= 0, "listedAmount is invalid: %d", listedAmount);

        final PriceQuote quote = MoneyExchange.secureQuote(listedCurrency, listedAmount);
        final PriceKey key = new PriceKey(buyerId, item);
        final Escrow escrow = new Escrow(sellerId, affiliateId, description, quote);
        _escrowCache.addEscrow(key, escrow);
        return quote;
    }

//    // TODO: remove
//    public int secureBarPrice (
//        final int memberId, final int creatorId, final int affiliateId,
//        final ItemIdent item, final int numBars, final String description)
//    {
//        Preconditions.checkArgument(!MemberName.isGuest(memberId), "Guests cannot secure prices.");
//        Preconditions.checkArgument(!MemberName.isGuest(creatorId), "Creators cannot be guests.");
//        Preconditions.checkArgument(item != null && (item.type != 0 || item.itemId != 0),
//            "item is invalid: %s", item.toString());
//        Preconditions.checkArgument(numBars >= 0, "bars is invalid: %d", numBars);
//
//        final PriceQuote quote = new PriceQuote(Currency.BARS, 0, numBars);
//        final PriceKey key = new PriceKey(memberId, item);
//        final Escrow escrow = new Escrow(creatorId, affiliateId, description, quote);
//        _escrowCache.addEscrow(key, escrow);
//        return 0;
//    }
//
//    // TODO: remove
//    public int secureCoinPrice (
//        final int memberId, final int creatorId, final int affiliateId,
//        final ItemIdent item, final int numCoins, final String description)
//    {
//        Preconditions.checkArgument(!MemberName.isGuest(memberId), "Guests cannot secure prices.");
//        Preconditions.checkArgument(!MemberName.isGuest(creatorId), "Creators cannot be guests.");
//        Preconditions.checkArgument(item != null && (item.type != 0 || item.itemId != 0),
//            "item is invalid: %s", item.toString());
//        Preconditions.checkArgument(numCoins >= 0, "numCoins is invalid: %d", numCoins);
//
//        // TODO: Use exchange rate to calculate bars.
//        final PriceQuote quote = new PriceQuote(Currency.COINS, numCoins, 0);
//        final PriceKey key = new PriceKey(memberId, item);
//        final Escrow escrow = new Escrow(creatorId, affiliateId, description, quote);
//        _escrowCache.addEscrow(key, escrow);
//        return 0;
//    }

    public void updateMoneyConfiguration (final MoneyConfiguration config)
    {
        // TODO Auto-generated method stub
    }

    public void init ()
    {
        _expirer.start();
    }

    private void logInPanopticon (
        final UserActionDetails info, final Currency currency,
        final double delta, final MemberAccountRecord account)
    {
        if (currency == Currency.COINS) {
            _eventLog.flowTransaction(info, (int)delta, account.getCoins());
        } else if (currency == Currency.BARS) {
            // TODO
        } else {
            // TODO: bling
        }
    }

    private UserActionDetails logUserAction (
        final int memberId, final int otherMemberId, final UserAction userAction,
        final ItemIdent item, final String description)
    {
        try {
            final UserActionDetails details = new UserActionDetails(
                memberId, userAction, otherMemberId,
                (item == null) ? Item.NOT_A_TYPE : item.type,
                (item == null) ? UserActionDetails.INVALID_ID : item.itemId,
                description);
            _userActionRepo.logUserAction(details);
            return details;
        } catch (final PersistenceException pe) {
            throw new RepositoryException(pe);
        }
    }

    private final MoneyHistoryExpirer _expirer;
    private final MsoyEventLogger _eventLog;
    private final UserActionRepository _userActionRepo;
    private final MoneyRepository _repo;
    private final EscrowCache _escrowCache;
}
