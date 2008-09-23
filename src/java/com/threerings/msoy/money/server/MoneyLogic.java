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
import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
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

import static com.threerings.msoy.Log.log;

/**
 * Facade for all money (coins, bars, and bling) transactions. This is the starting place to
 * access these services.
 *
 * TODO: transactional support- database transactions for proper rollback??
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
        MoneyRepository repo, PriceQuoteCache priceCache, UserActionRepository userActionRepo,
        MsoyEventLogger eventLog, MessageConnection conn, MemberRepository memberRepo, 
        ShutdownManager sm, @MainInvoker Invoker invoker, MoneyNodeActions nodeActions,
        BlingPoolDistributor blingDistributor, MoneyExchange exchange)
    {
        _repo = repo;
        _priceCache = priceCache;
        _userActionRepo = userActionRepo;
        _eventLog = eventLog;
        _expirer = new MoneyTransactionExpirer(repo, invoker, sm);
        _msgReceiver = new MoneyMessageReceiver(conn, this, memberRepo, sm, invoker);
        _nodeActions = nodeActions;
        _exchange = exchange;
        _blingDistributor = blingDistributor;
    }
    
    /**
     * Indicates that a member has earned some number of coins.  This will notify interested
     * clients that coins were earned, without actually awarding the coins yet.  Future calls to
     * {@link #awardCoins(int, int, int, ItemIdent, int, String, UserAction, boolean)} to award
     * the coins must use "true" for wasNotified to indicate the user was already notified of this.
     * 
     * @param memberId ID of the member who earned coins.
     * @param amount Number of coins earned.
     */
    public void notifyCoinsEarned (int memberId, int amount)
    {
        _nodeActions.moneyUpdated(memberId, Currency.COINS, amount);
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
     * @param wasNotified If true, an earlier call to {@link #notifyCoinsEarned(int, int)} was
     * made, so this call should not notify the user.
     * @return Result of the money operation.  The recipient of the coins, the creator, and the 
     * affiliate will all be included, if applicable.
     */
    @Retry(exception=StaleDataException.class)
    public MoneyResult awardCoins (
        int memberId, int creatorId, int affiliateId, ItemIdent item, int amount,
        boolean wasNotified, UserAction userAction, Object... args)
    {
        Preconditions.checkArgument(!MemberName.isGuest(memberId), "Cannot award coins to guests.");
        Preconditions.checkArgument(amount >= 0, "amount is invalid: %d", amount);
        Preconditions.checkArgument(
            (item == null) || (item.type != Item.NOT_A_TYPE && item.itemId != 0),
            "item is invalid: %s", item);

        final String description = MessageBundle.tcompose(userAction.getMessage(), args);

        MemberAccountRecord account = _repo.getAccountById(memberId);
        final MoneyTransactionRecord history = account.awardCoins(amount, item, description);
        _repo.saveAccount(account);
        _repo.addTransaction(history);

        final UserActionDetails info = logUserAction(memberId, 0, userAction,
            // Handle the mystical case where games need to be logged specially for
            // humanity assessment
            // TODO: Handle AVRG's COMPLETED_QUEST here too?
            (userAction == UserAction.PLAYED_GAME) ? args[1].toString() + args[2].toString() :
            description, item);
        logInPanopticon(info, Currency.COINS, amount, account);

        if (!wasNotified) {
            _nodeActions.moneyUpdated(history);
        }
        
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
        final MoneyTransactionRecord history = account.buyBars(numBars, description);
        _repo.saveAccount(account);
        _repo.addTransaction(history);

        logUserAction(memberId, UserActionDetails.INVALID_ID, UserAction.BOUGHT_BARS,
            history.description);

        _nodeActions.moneyUpdated(history);
        
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
     * @param authedAmount the amount the buyer has validated to purchase the item.
     * @param isSupport is the buyer a member of the support staff?
     * @throws NotSecuredException iff there is no secured price for the item and the authorized
     * buy amount is not enough money.
     */
    public MoneyResult buyItem (
        final MemberRecord buyerRec, CatalogIdent item, int creatorId, String description,
        Currency listedCurrency, int listedAmount, Currency buyCurrency, int authedAmount)
        throws NotEnoughMoneyException, NotSecuredException
    {
        Preconditions.checkArgument(isValid(item), "item is invalid: %s", item);
        Preconditions.checkArgument(
            buyCurrency == Currency.BARS || buyCurrency == Currency.COINS,
            "buyCurrency is invalid: %s", buyCurrency);

        int buyerId = buyerRec.memberId;

        // Get the secured prices for the item.
        PriceKey key = new PriceKey(buyerId, item);
        PriceQuote quote = _priceCache.getQuote(key);
        if (quote == null || quote.getAmount(buyCurrency) > authedAmount) {
            // In the unlikely scenarios that there was either no secured price (expired) or
            // they provided an out-of-date authed amount, we go ahead and secure a new price
            // right now and see if that works. 
            quote = securePrice(buyerId, item, listedCurrency, listedAmount);
            if (quote.getAmount(buyCurrency) > authedAmount) {
                // doh, it doesn't work, so we need to tell them about this new latest price
                // we've secured for them
                throw new NotSecuredException(buyerId, item, quote);
            }
        }

        // Note that from here on, we're going to use the buyCost, which *could* be lower
        // than what the user authorized. Good for them.
        int buyCost = quote.getAmount(buyCurrency);

        // Get buyer account...
        MemberAccountRecord buyer = _repo.getAccountById(buyerId);

        // see if the user can afford it, or if we'll let them have it for free (support+)
        boolean magicFreeItem;
        if (buyCost > buyer.getAmount(buyCurrency)) {
            magicFreeItem = buyerRec.isSupport();
            if (!magicFreeItem) {
                throw new NotEnoughMoneyException(
                    buyerId, buyCurrency, buyCost, buyer.getAmount(buyCurrency));
            }
        } else {
            magicFreeItem = false;
        }

        // see what kind of payouts we're going pay- null means don't load, don't care
        CurrencyAmount creatorPayout = magicFreeItem ? null : computePayout(false, quote);
        CurrencyAmount affiliatePayout = magicFreeItem ? null : computePayout(true, quote);

        // Get creator, if applicable
        MemberAccountRecord creator;
        if (creatorPayout == null) {
            creator = null;

        } else if (buyerId == creatorId) {
            creator = buyer;

        } else {
            creator = _repo.getAccountById(creatorId);
        }

        // load the buyer's affiliate
        int affiliateId;
        if (DeploymentConfig.barsEnabled) {
            affiliateId = buyerRec.affiliateMemberId;
        } else {
            affiliateId = 0;
        }
        MemberAccountRecord affiliate;
        if (affiliatePayout == null || affiliateId == 0) {
            affiliate = null;

        } else if (affiliateId == buyerId) { // this would be weird, but let's handle it
            affiliate = buyer;

        } else if (affiliateId == creatorId) {
            affiliate = creator;

        } else {
            affiliate = _repo.getAccountById(affiliateId);
        }

        // add a transaction for the buyer
        MoneyTransactionRecord buyerTrans = buyer.buyItem(
            buyCurrency, magicFreeItem ? 0 : buyCost,
            MessageBundle.tcompose(magicFreeItem ? "m.item_magicfree" : "m.item_bought",
                description, item.type, item.catalogId),
            item);
        _repo.addTransaction(buyerTrans);

        // log userAction and to panopticon for the buyer
        UserActionDetails info = logUserAction(buyerId, UserActionDetails.INVALID_ID,
            UserAction.BOUGHT_ITEM, description, item);
        logInPanopticon(info, buyCurrency, buyerTrans.amount, buyer);

        // add a transaction for the creator
        MoneyTransactionRecord creatorTrans;
        if (creator == null) { // creator is null when creatorPayout is null
            creatorTrans = null;

        } else {
            creatorTrans = creator.payout(
                TransactionType.CREATOR_PAYOUT, creatorPayout.currency, creatorPayout.amount,
                MessageBundle.tcompose("m.item_sold",
                    description, item.type, item.catalogId),
                item, buyerTrans.id);
            _repo.addTransaction(creatorTrans);

            // log userAction and to panopticon for the creator
            info = logUserAction(creatorId, buyerId,
                UserAction.RECEIVED_PAYOUT, description, item);
            logInPanopticon(info, buyCurrency, creatorTrans.amount, creator);
        }

        // add a transaction for the affiliate
        MoneyTransactionRecord affiliateTrans;
        if (affiliate == null) { // no affiliate or no affiliatePayout
            affiliateTrans = null;

        } else {
            affiliateTrans = affiliate.payout(
                TransactionType.AFFILIATE_PAYOUT, affiliatePayout.currency, affiliatePayout.amount,
                MessageBundle.tcompose("m.item_affiliate", buyerRec.name, buyerRec.memberId),
                item, buyerTrans.id);
            _repo.addTransaction(affiliateTrans);

            // log userAction and to panopticon for the affiliate
            info = logUserAction(affiliateId, buyerId,
                UserAction.RECEIVED_PAYOUT, description, item);
            logInPanopticon(info, buyCurrency, affiliateTrans.amount, affiliate);
        }

        // save stuff off
        _repo.saveAccount(buyer);
        _nodeActions.moneyUpdated(buyerTrans);
        if ((creator != null) && (creatorId != buyerId)) {
            _repo.saveAccount(creator);
            _nodeActions.moneyUpdated(creatorTrans);
        }
        if ((affiliate != null) && (affiliateId != buyerId) && (affiliateId != creatorId)) {
            _repo.saveAccount(affiliate);
            _nodeActions.moneyUpdated(affiliateTrans);
        }

        // The item no longer needs to be in the cache.
        _priceCache.removeQuote(key);
        // Inform the exchange that we've actually made the exchange
        if (!magicFreeItem) {
            _exchange.processPurchase(quote, buyCurrency);
        }

        return new MoneyResult(buyer.getMemberMoney(),
            (creator == null) ? null : creator.getMemberMoney(),
            (affiliate == null) ? null : affiliate.getMemberMoney(),
            buyerTrans.toMoneyTransaction(),
            (creatorTrans == null) ? null : creatorTrans.toMoneyTransaction(),
            (affiliateTrans == null) ? null : affiliateTrans.toMoneyTransaction());
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
     * @param blingAmount Amount of bling (NOT centibling) to convert to bars.
     * @return Number of bars added to the account.
     * @throws NotEnoughMoneyException The account does not have the specified amount of bling
     * available, aight?
     */
    public void exchangeBlingForBars (int memberId, int blingAmount)
        throws NotEnoughMoneyException
    {
        // Update the account record, adding the amount amount specified to bars, while subtracting
        // the amount (after converting it to centibling) from the bling column.
        int rows = _repo.exchangeBlingForBars(memberId, blingAmount);
        int centibling = blingAmount * 100;
        
        // Get the newly updated account record.  If the above did not update any rows, this will
        // have the old values.
        MemberAccountRecord account = _repo.getAccountById(memberId);
        
        // If no rows updated, assume that they never had enough bling in their account.  Could
        // also be the member account wasn't found, which means they don't have any bling anyway.
        if (rows == 0) {
            throw new NotEnoughMoneyException(memberId, Currency.BLING, centibling, 
                account.bling);
        }
        
        // Create two transaction records, one for the amount deducted, and one for the amount added.
        MoneyTransactionRecord tx1 = new MoneyTransactionRecord(memberId, 
            TransactionType.SPENT_FOR_EXCHANGE, Currency.BLING, -centibling, account.bling, 
            MessageBundle.tcompose("m.exchange_spent", blingAmount), null);
        MoneyTransactionRecord tx2 = new MoneyTransactionRecord(memberId, 
            TransactionType.RECEIVED_FROM_EXCHANGE, Currency.BARS, blingAmount, account.bars, 
            MessageBundle.tcompose("m.exchange_added", blingAmount), null);
        _repo.addTransaction(tx1);
        _repo.addTransaction(tx2);
    }

    /**
     * Retrieves the amount that a member's current bling is worth in American dollars.
     *
     * @param bling The amount of bling (NOT centibling) to get the worth of.
     * @return The amount the bling is worth in USD cents.
     */
    public int getBlingWorth (int bling)
    {
        // Bling is 100x the actual bling value, and since we're returning pennies, blingWorth
        // (measured in dollars) is 0.01x the value we want.  So it balances out.
        return (int)(bling * RuntimeConfig.server.blingWorth);
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

        return _repo.getAccountById(memberId).getMemberMoney();
    }

    /**
     * Secures a price for an item. This ensures the user will be able to purchase an item
     * for a set price. This price will remain available for some amount of time (specified by
     * {@link MoneyConfiguration#getSecurePriceDuration()}. The secured price may also be removed
     * if the maximum number of secured prices system-wide has been reached (specified by
     * {@link MoneyConfiguration#getMaxSecuredPrices()}. In either case, an attempt to buy the
     * item will fail with a {@link NotSecuredException}. If a guest id is specified, the quote
     * is returned, but not saved in the cache.
     *
     * @param buyerId the memberId of the buying user.
     * @param item the identity of the catalog listing.
     * @param listedCurrency the currency at which the item is listed.
     * @param listedAmount the amount at which the item is listed.
     * @return A full PriceQuote for the item.
     */
    public PriceQuote securePrice (
        int buyerId, CatalogIdent item, Currency listedCurrency, int listedAmount)
    {
        Preconditions.checkArgument(isValid(item), "item is invalid: %s", item);
        Preconditions.checkArgument(listedAmount >= 0, "listedAmount is invalid: %d", listedAmount);

        final PriceQuote quote = _exchange.secureQuote(listedCurrency, listedAmount);
        if (!MemberName.isGuest(buyerId)) {
            final PriceKey key = new PriceKey(buyerId, item);
            _priceCache.addQuote(key, quote);
        }
        return quote;
    }

    /**
     * Initializes the money service by starting up required services, such as an expiration
     * monitor and queue listeners. This method is idempotent.
     */
    public void init ()
    {
        _msgReceiver.start();
        
        // Bling distributor should only be started if bars are enabled.
        if (DeploymentConfig.barsEnabled) {
            _blingDistributor.start();
        }
    }

    /**
     * Is this CatalogIdent valid?
     */
    protected static boolean isValid (CatalogIdent ident)
    {
        return (ident != null) && (ident.type != Item.NOT_A_TYPE) && (ident.catalogId != 0);
    }

    protected CurrencyAmount computePayout (boolean affiliate, PriceQuote quote)
    {
        Currency currency;
        int amount;
        float percentage = affiliate ? RuntimeConfig.server.affiliatePercentage
                                     : RuntimeConfig.server.creatorPercentage;
        switch (quote.getListedCurrency()) {
        case COINS:
            currency = Currency.COINS;
            amount = (int) Math.floor(quote.getCoins() * percentage);
            break;

        case BARS:
            currency = Currency.BLING;
            // bars are equal to bling, but we actually track "centibling"
            amount = (int) Math.floor(quote.getBars() * 100 * percentage);
            break;

        default:
            throw new RuntimeException();
        }

        // for creators, we pay out "0" so that they get a sales report,
        // but we never do that for affiliates
        if ((amount == 0) && affiliate) {
            return null;
        }

        return new CurrencyAmount(currency, amount);
    }

    protected void logInPanopticon (
        UserActionDetails info, Currency currency, int delta, MemberAccountRecord account)
    {
        switch (currency) {
        case COINS:
            _eventLog.flowTransaction(info, delta, account.coins);
            break;

        case BARS:
            log.info("TODO: log bars to panopticon");
            break;

        case BLING:
            log.info("TODO: log bling to panopticon");
            break;
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

    protected final MoneyExchange _exchange;
    protected final MoneyTransactionExpirer _expirer;
    protected final MsoyEventLogger _eventLog;
    protected final UserActionRepository _userActionRepo;
    protected final MoneyRepository _repo;
    protected final PriceQuoteCache _priceCache;
    protected final MoneyMessageReceiver _msgReceiver;
    protected final MoneyNodeActions _nodeActions;
    protected final BlingPoolDistributor _blingDistributor;
}
