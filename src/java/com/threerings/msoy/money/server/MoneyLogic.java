//
// $Id$

package com.threerings.msoy.money.server;

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
import com.samskivert.util.Invoker;
import com.threerings.presents.annotation.BlockingThread;
import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.server.ShutdownManager;
import com.threerings.util.MessageBundle;

import com.threerings.messaging.MessageConnection;
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
import com.threerings.msoy.money.server.impl.Escrow;
import com.threerings.msoy.money.server.impl.EscrowCache;
import com.threerings.msoy.money.server.impl.MoneyHistoryExpirer;
import com.threerings.msoy.money.server.impl.MoneyMessageReceiver;
import com.threerings.msoy.money.server.impl.PriceKey;
import com.threerings.msoy.money.server.impl.Retry;
import com.threerings.msoy.money.server.persist.MemberAccountHistoryRecord;
import com.threerings.msoy.money.server.persist.MemberAccountRecord;
import com.threerings.msoy.money.server.persist.MoneyRepository;
import com.threerings.msoy.money.server.persist.PersistentCurrency;
import com.threerings.msoy.money.server.persist.PersistentTransactionType;
import com.threerings.msoy.money.server.persist.StaleDataException;

/**
 * Facade for all money (coins, bars, and bling) transactions. This is the starting place to
 * access these services.
 *
 * TODO: transactional support
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 * @author Ray Greenwell <ray@threerings.net>
 */
@Singleton
@BlockingThread
public class MoneyLogic
{
    @Inject
    public MoneyLogic (
        final MoneyRepository repo, final EscrowCache escrowCache,
        final UserActionRepository userActionRepo,final MsoyEventLogger eventLog, 
        final MessageConnection conn, final ShutdownManager sm, @MainInvoker final Invoker invoker)
    {
        _repo = repo;
        _escrowCache = escrowCache;
        _userActionRepo = userActionRepo;
        _eventLog = eventLog;
        _expirer = new MoneyHistoryExpirer(repo, sm, invoker);
        _msgReceiver = new MoneyMessageReceiver(conn, this, sm, invoker);
    }

    /**
     * Awards some number of coins to a member for some activity, such as playing a game. This
     * will also keep track of coins spent awarded for each creator, so that the creators can
     * receive bling when people play their games.
     *
     * @param memberId ID of the member to receive the coins.
     * @param creatorId ID of the creator of the item that caused the coins to be awarded.
     * @param affiliateId ID of the affiliate associated with the transaction. Zero if no
     * affiliate.
     * @param item Optional item that coins were awarded for (i.e. a game)
     * @param amount Number of coins to be awarded.
     * @param description Description that will appear in the user's transaction history.
     * @param userAction The user action that caused coins to be awarded.
     * @return Map containing member ID to the money that member has in the account. The recipient
     * of the coins, the creator, and the affiliate will all be included, if applicable.
     */
    @Retry(exception=StaleDataException.class)
    public MoneyResult awardCoins (
        final int memberId, final int creatorId, final int affiliateId,
        final ItemIdent item, final int amount, final String description, final
        UserAction userAction)
    {
        Preconditions.checkArgument(!MemberName.isGuest(memberId), "Cannot award coins to guests.");
        Preconditions.checkArgument(amount >= 0, "amount is invalid: %d", amount);
        Preconditions.checkArgument(
            (item == null) || (item.type != Item.NOT_A_TYPE && item.itemId != 0),
            "item is invalid: %s", item);

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

    /**
     * The member has purchased some number of bars. This will add the number of bars to their
     * account, fulfilling the transaction.
     *
     * @param memberId ID of the member receiving bars.
     * @param numBars Number of bars to add to their account.
     * @return The money the member now has in their account.
     */
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
            history.description);

        return new MoneyResult(account.getMemberMoney(), null, null, 
            history.createMoneyHistory(null), null, null);
    }

    /**
     * Purchases an item. This will only update the appropriate
     * accounts of an exchange of money -- item fulfillment must be handled separately.
     *
     * @param buyer the member record of the buying user.
     * @param item the identity of the catalog listing.
     * @param listedCurrency the currency at which the item is listed.
     * @param listedAmount the amount at which the item is listed.
     * @param buyType the currency the buyer is using
     * @param buyAmount the amount the buyer has validated to purchase the item.
     * @param isSupport is the buyer a member of the support staff?
     * @throws NotSecuredException iff there is no secured price for the item and the authorized
     * buy amount is not enough money.
     */
    @Retry(exception=StaleDataException.class)
    public MoneyResult buyItem (
        final MemberRecord buyrec, CatalogIdent item, Currency listedCurrency, int listedAmount,
        Currency buyCurrency, int buyAmount)
        throws NotEnoughMoneyException, NotSecuredException
    {
        Preconditions.checkArgument(isValid(item), "item is invalid: %s", item);
        Preconditions.checkArgument(
            buyCurrency == Currency.BARS || buyCurrency == Currency.COINS,
            "buyCurrency is invalid: %s", buyCurrency);

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
            amount -= (int)(_config.getCreatorPercentage() * amount);
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
            creatorHistory = creator.creatorPayout(
                quote.getListedCurrency(), (int)history.amount,
                // TODO: fuck me friday, I'm not sure how this will actually xlate in GWT land...
                MessageBundle.tcompose("m.item_bought", escrow.getDescription()),
                item, _config.getCreatorPercentage(), history.id);
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
        return new MoneyResult(buyer.getMemberMoney(),
            buyerIsCreator ? null : creator.getMemberMoney(),
            null, mh, buyerIsCreator ? null : creatorHistory.createMoneyHistory(mh), null);
    }

    /**
     * Deducts some amount of bling from the member's account. This will be used by CSR's for
     * corrective actions or when the member chooses to cash out their bling.
     */
    public void deductBling (final int memberId, final double amount)
    {
        // TODO Auto-generated method stub
    }

    /**
     * Converts some amount of bling in a member's account into bars.
     *
     * @param memberId ID of the member.
     * @param blingAmount Amount of bling to convert to bars.
     * @return Number of bars added to the account.
     * @throws NotEnoughMoneyException The account does not have the specified amount of bling
     * available, aight?
     */
    public int exchangeBlingForBars (final int memberId, final double blingAmount)
        throws NotEnoughMoneyException
    {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Retrieves the amount that a member's current bling is worth in American dollars.
     *
     * @param memberId ID of the member to retrieve bling for.
     * @return The amount the bling is worth in American dollars.
     */
    public BigDecimal getBlingWorth (final int memberId)
    {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Retrieves a money transaction history for a member, including one or more money types. The
     * returned list is sorted by transaction date ascending. A portion of the log can be returned
     * at a time for pagination.
     *
     * @param memberId ID of the member to retrieve money for.
     * @param currency Money type to retrieve logs for. If null, then records for all money types are
     * returned.
     * @param transactionTypes Set of transaction types to retrieve logs for.  If null, all
     *      transactionTypes will be retrieved.
     * @param start Zero-based index of the first log item to return.
     * @param count The number of log items to return. If Integer.MAX_VALUE, this will return all
     * records.
     * @param descending If true, the log will be sorted by transaction date descending.
     * @return List of requested past transactions.
     */
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

    /**
     * Retrieves the total number of transaction history entries we have stored for this query.
     *
     * @param memberId ID of the member to count transactions for.
     * @param currency Currency to retrieve logs for. If null, then records for all money types are
     * counted.
     * @param transactionTypes Set of transaction types to retrieve logs for.  If null, all
     *      transactionTypes will be counted.
     */
    public int getHistoryCount (
        final int memberId, final Currency currency, final EnumSet<TransactionType> transactionTypes)
    {
        return _repo.getHistoryCount(memberId, PersistentCurrency.fromCurrency(currency),
            toPersist(transactionTypes));
    }

    /**
     * Retrieves the current account balance (coins, bars, and bling) for the given member.
     *
     * @param memberId ID of the member to retrieve money for.
     * @return The money in their account.
     */
    public MemberMoney getMoneyFor (final int memberId)
    {
        Preconditions.checkArgument(!MemberName.isGuest(memberId),
            "Cannot retrieve money info for guests.");

        final MemberAccountRecord account = _repo.getAccountById(memberId);
        return account != null ? account.getMemberMoney() : new MemberMoney(memberId);
    }

    /**
     * Secures a price for an item. This ensures the user will be able to purchase an item
     * for a set price. This price will remain available for some amount of time (specified by
     * {@link MoneyConfiguration#getSecurePriceDuration()}. The secured price may also be removed
     * if the maximum number of secured prices system-wide has been reached (specified by
     * {@link MoneyConfiguration#getMaxSecuredPrices()}. In either case, an attempt to buy the
     * item will fail with a {@link NotSecuredException}.
     *
     * @param buyerId the memberId of the buying user.
     * @param item the identity of the catalog listing.
     * @param listedCurrency the currency at which the item is listed.
     * @param listedAmount the amount at which the item is listed.
     * @param creatorId the memberId of the seller.
     * @param affiliateId the id of the affiliate associated with the purchase. Uhm..
     * @param description A description of the item that will appear on the member's transaction
     * history if purchased.
     * @return A full PriceQuote for the item.
     */
    public PriceQuote securePrice (
        int buyerId, CatalogIdent item, Currency listedCurrency, int listedAmount,
        int sellerId, int affiliateId, String description)
    {
        Preconditions.checkArgument(!MemberName.isGuest(buyerId), "Guests cannot secure prices.");
        Preconditions.checkArgument(!MemberName.isGuest(sellerId), "Creators cannot be guests.");
        Preconditions.checkArgument(isValid(item), "item is invalid: %s", item);
        Preconditions.checkArgument(listedAmount >= 0, "listedAmount is invalid: %d", listedAmount);

        final PriceQuote quote = MoneyExchange.secureQuote(listedCurrency, listedAmount);
        final PriceKey key = new PriceKey(buyerId, item);
        final Escrow escrow = new Escrow(sellerId, affiliateId, description, quote);
        _escrowCache.addEscrow(key, escrow);
        return quote;
    }

    /**
     * Initializes the money service by starting up required services, such as an expiration
     * monitor and queue listeners. This method is idempotent.
     */
    public void init ()
    {
        _expirer.start();
        _msgReceiver.start();
    }

    /**
     * Is this CatalogIdent valid?
     */
    protected static boolean isValid (CatalogIdent ident)
    {
        return (ident != null) && (ident.type != Item.NOT_A_TYPE) && (ident.catalogId != 0);
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

    protected void logInPanopticon (
        final UserActionDetails info, final Currency currency,
        final double delta, final MemberAccountRecord account)
    {
        if (currency == Currency.COINS) {
            _eventLog.flowTransaction(info, (int)delta, account.coins);
        } else if (currency == Currency.BARS) {
            // TODO
        } else {
            // TODO: bling
        }
    }

    protected UserActionDetails logUserAction (
        final int memberId, final int otherMemberId, final UserAction userAction,
        final ItemIdent item, final String description)
    {
        final UserActionDetails details = new UserActionDetails(
            memberId, userAction, otherMemberId,
            (item == null) ? Item.NOT_A_TYPE : item.type,
            (item == null) ? UserActionDetails.INVALID_ID : item.itemId,
            description);
        _userActionRepo.logUserAction(details);
        return details;
    }

    @Inject protected MoneyConfiguration _config;

    protected final MoneyHistoryExpirer _expirer;
    protected final MsoyEventLogger _eventLog;
    protected final UserActionRepository _userActionRepo;
    protected final MoneyRepository _repo;
    protected final EscrowCache _escrowCache;
    protected final MoneyMessageReceiver _msgReceiver;
}
