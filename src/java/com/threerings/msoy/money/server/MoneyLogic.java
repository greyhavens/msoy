//
// $Id$

package com.threerings.msoy.money.server;

import java.util.EnumSet;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.Invoker;

import com.threerings.presents.annotation.BlockingThread;
import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.server.ShutdownManager;

import com.threerings.util.MessageBundle;

import com.threerings.messaging.MessageConnection;
import com.threerings.msoy.admin.server.RuntimeConfig;
import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.data.UserActionDetails;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.UserActionRepository;
import com.threerings.msoy.server.util.Retry;

import com.threerings.msoy.item.data.all.CatalogIdent;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;

import com.threerings.msoy.money.data.all.MemberMoney;
import com.threerings.msoy.money.data.all.MoneyTransaction;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.PriceQuote;
import com.threerings.msoy.money.data.all.TransactionType;
import com.threerings.msoy.money.server.persist.MoneyTransactionRecord;
import com.threerings.msoy.money.server.persist.MemberAccountRecord;
import com.threerings.msoy.money.server.persist.MoneyRepository;
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
        MoneyRepository repo, EscrowCache escrowCache, UserActionRepository userActionRepo,
        MsoyEventLogger eventLog, MessageConnection conn, ShutdownManager sm,
        @MainInvoker Invoker invoker)
    {
        _repo = repo;
        _escrowCache = escrowCache;
        _userActionRepo = userActionRepo;
        _eventLog = eventLog;
        _expirer = new MoneyTransactionExpirer(repo, invoker, sm);
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
        int memberId, int creatorId, int affiliateId, ItemIdent item, int amount,
        String description, UserAction userAction)
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
        final MoneyTransactionRecord history = account.awardCoins(amount, item, description);
        _repo.saveAccount(account);
        _repo.addTransaction(history);

        // TODO: creator and affiliate

        // TODO: what the fuck is going on here, copy/paste dupe bug?
        logUserAction(memberId, UserActionDetails.INVALID_ID, userAction, description, item);
        final UserActionDetails info = logUserAction(memberId, 0, userAction, description, item);
        logInPanopticon(info, Currency.COINS, amount, account);
        
        return new MoneyResult(account.getMemberMoney(), null, null, 
            history.toMoneyTransaction(), null, null);
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
        final MoneyTransactionRecord history = account.buyBars(numBars, description);
        _repo.saveAccount(account);
        _repo.addTransaction(history);

        logUserAction(memberId, UserActionDetails.INVALID_ID, UserAction.BOUGHT_BARS,
            history.description);

        return new MoneyResult(account.getMemberMoney(), null, null, 
            history.toMoneyTransaction(), null, null);
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
        final MemberRecord buyerRec, CatalogIdent item, Currency listedCurrency, int listedAmount,
        Currency buyCurrency, int buyAmount)
        throws NotEnoughMoneyException, NotSecuredException
    {
        Preconditions.checkArgument(isValid(item), "item is invalid: %s", item);
        Preconditions.checkArgument(
            buyCurrency == Currency.BARS || buyCurrency == Currency.COINS,
            "buyCurrency is invalid: %s", buyCurrency);

        // Get the secured prices for the item.
        final PriceKey key = new PriceKey(buyerRec.memberId, item);
        final Escrow escrow = _escrowCache.getEscrow(key);
        if (escrow == null) {
            // TODO: 
            // What we need to do here is also have the sellerId, affiliateId, and description..
            // so that we can secure a price. If the buyAmount is at least the priceQuote,
            // we can go ahead and process the purchase here. Otherwise we need to throw
            // the exception with the new PriceQuote inside it! Why not! Do the fucking thing
            // that will need to be done anyway!
            throw new NotSecuredException(buyerRec.memberId, item);
        }
        final PriceQuote quote = escrow.getQuote();

        // check to see if they submitted a valid buyAmount
        if (!quote.isSatisfied(buyCurrency, buyAmount)) {
            // TODO: see TODO note above, only here we actually have a quote already secured
            // (And we should either get a new quote, or just leave the one in the cache,
            // but it would be an exploit to extend the lifetime of the quote)
            // AND: we should return the new PriceQuote with this error, so that it can
            // make it's way to the client. Just like the note above...
            throw new NotSecuredException(buyerRec.memberId, item);
        }

        // Get buyer account...
        final MemberAccountRecord buyer = _repo.getAccountById(buyerRec.memberId);
        int hasAmount = (buyer == null) ? 0 : buyer.getAmount(buyCurrency);

        // support personnel can adjust the price..
        // magically adjust for support personnel
        if ((buyer != null) && buyerRec.isSupport() && (buyAmount > hasAmount)) {
            // update their desired buy price to reflect the amount they have
            buyAmount = hasAmount;

        } else if (buyer == null || !buyer.canAfford(buyCurrency, buyAmount)) {
            throw new NotEnoughMoneyException(buyerRec.memberId, buyCurrency, buyAmount, hasAmount);
        }

        // TODO: we need to update PriceQuote to reflect the actual purchase price.. Right?
        // TODO: actually, below, I'm just not using it. Revisit this soon.

        // TODO: move this until after the purchase... this WHOLE fucking method should
        // accept a Runnable, maybe, to process the purchase
        // Inform the exchange that we've actually made the exchange
        MoneyExchange.processPurchase(quote, buyCurrency, buyAmount);

        // If the creator is buying their own item, don't give them a payback, and deduct the amount
        // they would have received.
        int creatorId = escrow.getCreatorId();
        boolean buyerIsCreator = (buyerRec.memberId == creatorId);

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

        // add a transaction for the buyer
        MoneyTransactionRecord buyerTrans = buyer.buyItem(buyCurrency, buyAmount,
            MessageBundle.tcompose("m.itemBought",
                escrow.getDescription(), item.type, item.catalogId),
            item);
        _repo.addTransaction(buyerTrans);

        // Extra logging bullshit
        UserActionDetails info = logUserAction(buyerRec.memberId, UserActionDetails.INVALID_ID,
            UserAction.BOUGHT_ITEM, escrow.getDescription(), item);
        logInPanopticon(info, buyCurrency, buyerTrans.amount, buyer);

        // add a transaction for the creator
        MoneyTransactionRecord creatorTrans = creator.creatorPayout(
            quote.getListedCurrency(), buyCurrency, buyAmount,
            MessageBundle.tcompose("m.itemSold",
                escrow.getDescription(), item.type, item.catalogId),
            item, RuntimeConfig.server.creatorPercentage, buyerTrans.id);
        _repo.addTransaction(creatorTrans);

        // Extra logging bullshit
        info = logUserAction(creatorId, buyerRec.memberId, UserAction.RECEIVED_PAYOUT,
            escrow.getDescription(), item);
        logInPanopticon(info, buyCurrency, creatorTrans.amount, creator);

        // TODO: affiliate cut, as soon as affiliate ids are de-fuckola'd

        // save stuff off
        _repo.saveAccount(buyer);
        if (!buyerIsCreator) {
            _repo.saveAccount(creator);
        }

        // The item no longer needs to be in the cache.
        _escrowCache.removeEscrow(key);

        return new MoneyResult(buyer.getMemberMoney(), creator.getMemberMoney(), null /* TODO */,
            buyerTrans.toMoneyTransaction(), creatorTrans.toMoneyTransaction(), null /* TODO */);
    }

    /**
     * Deducts some amount of bling from the member's account. This will be used by CSR's for
     * corrective actions or when the member chooses to cash out their bling.
     */
    public void deductBling (int memberId, int amount)
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
    public int exchangeBlingForBars (int memberId, int blingAmount)
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
    public int getBlingWorth (int memberId)
    {
        // TODO Auto-generated method stub
        return 0;
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
    public List<MoneyTransaction> getTransactions (
        int memberId, EnumSet<TransactionType> transactionTypes, Currency currency,
        int start, int count, boolean descending)
    {
        Preconditions.checkArgument(!MemberName.isGuest(memberId),
            "Cannot retrieve money log for guests.");
        Preconditions.checkArgument(start >= 0, "start is invalid: %d", start);
        Preconditions.checkArgument(count > 0, "count is invalid: %d", count);

        // I think this won't work, because the list returned is special..
//        return Lists.transform(
//            _repo.getTransactions(memberId, transactionTypes, currency, start, count, descending),
//            MoneyTransactionRecord.TO_TRANSACTION);

        return Lists.newArrayList(Iterables.transform(
            _repo.getTransactions(memberId, transactionTypes, currency, start, count, descending),
            MoneyTransactionRecord.TO_TRANSACTION));
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
    public int getTransactionCount (
        int memberId, EnumSet<TransactionType> transactionTypes, Currency currency)
    {
        return _repo.getTransactionCount(memberId, transactionTypes, currency);
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
        _msgReceiver.start();
    }

    /**
     * Is this CatalogIdent valid?
     */
    protected static boolean isValid (CatalogIdent ident)
    {
        return (ident != null) && (ident.type != Item.NOT_A_TYPE) && (ident.catalogId != 0);
    }

    protected void logInPanopticon (
        UserActionDetails info, Currency currency, int delta, MemberAccountRecord account)
    {
        if (currency == Currency.COINS) {
            _eventLog.flowTransaction(info, delta, account.coins);
        } else if (currency == Currency.BARS) {
            // TODO
        } else {
            // TODO: bling
        }
    }

    protected UserActionDetails logUserAction (
        int memberId, int otherMemberId, UserAction userAction, String description)
    {
        return logUserAction(memberId, otherMemberId, userAction, description, (ItemIdent)null);
    }

    protected UserActionDetails logUserAction (
        int memberId, int otherMemberId, UserAction userAction, String description,
        ItemIdent item)
    {
        UserActionDetails details = new UserActionDetails(
            memberId, userAction, otherMemberId,
            (item == null) ? Item.NOT_A_TYPE : item.type,
            (item == null) ? UserActionDetails.INVALID_ID : item.itemId,
            description);
        _userActionRepo.logUserAction(details);
        return details;
    }

    protected UserActionDetails logUserAction (
        int memberId, int otherMemberId, UserAction userAction, String description,
        CatalogIdent catIdent)
    {
        ItemIdent item = null;
        if (catIdent != null) {
            item = new ItemIdent(catIdent.type, catIdent.catalogId);
        }
        return logUserAction(memberId, otherMemberId, userAction, description, item);
    }

    protected final MoneyTransactionExpirer _expirer;
    protected final MsoyEventLogger _eventLog;
    protected final UserActionRepository _userActionRepo;
    protected final MoneyRepository _repo;
    protected final EscrowCache _escrowCache;
    protected final MoneyMessageReceiver _msgReceiver;
}
