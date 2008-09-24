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
import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.server.persist.UserActionRepository;
import com.threerings.msoy.server.util.Retry;

import com.threerings.msoy.item.data.all.CatalogIdent;
import com.threerings.msoy.item.data.all.Item;

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
     * Create the a money account for the specified user.
     */
    public void createMoneyAccount (int memberId, int creationAwardCoins)
    {
        _repo.create(memberId);
        if (creationAwardCoins > 0) {
            awardCoins(memberId, creationAwardCoins, false, UserAction.createdAccount(memberId));
        }
    }

    /**
     * Retrieves the current account balance (coins, bars, and bling) for the given member.
     *
     * @param memberId ID of the member to retrieve money for.
     * @return The money in their account.
     */
    public MemberMoney getMoneyFor (int memberId)
    {
        Preconditions.checkArgument(!MemberName.isGuest(memberId),
            "Cannot retrieve money info for guests.");

        return _repo.load(memberId).getMemberMoney();
    }

    /**
     * Indicates that a member has earned some number of coins.  This will notify interested
     * clients that coins were earned, without actually awarding the coins yet.  Future calls to
     * {@link #awardCoins(int, int, boolean, UserAction)} to award
     * the coins must use "false" for notify to indicate the user was already notified of this.
     *
     * @param memberId ID of the member who earned coins.
     * @param amount Number of coins earned.
     */
    public void notifyCoinsEarned (int memberId, int amount)
    {
        _nodeActions.moneyUpdated(memberId, Currency.COINS, amount);
    }

    /**
     * Awards some number of coins to a member for some activity, such as playing a game. This will
     * also keep track of coins spent awarded for each creator, so that the creators can receive
     * bling when people play their games.
     *
     * @param memberId ID of the member to receive the coins.
     * @param amount Number of coins to be awarded.
     * @param notify If false, an earlier call to {@link #notifyCoinsEarned(int, int)} was made, so
     * this call should not notify the user.
     * @param action The user action that caused coins to be awarded.
     */
    public MoneyTransaction awardCoins (int memberId, int amount, boolean notify, UserAction action)
    {
        Preconditions.checkArgument(!MemberName.isGuest(memberId), "Cannot award coins to guests.");
        Preconditions.checkArgument(amount >= 0, "amount is invalid: %d", amount);

        MoneyTransactionRecord tx = _repo.accumulateAndStoreTransaction(
            memberId, Currency.COINS, amount, TransactionType.AWARD, action.description, null);
        if (notify) {
            _nodeActions.moneyUpdated(tx);
        }
        logAction(action, tx);

        return tx.toMoneyTransaction();
    }

    /**
     * The member has purchased some number of bars. This will add the number of bars to their
     * account, fulfilling the transaction.
     *
     * @param memberId ID of the member receiving bars.
     * @param numBars Number of bars to add to their account.
     * @param description a translatable string that will be recorded along with the money
     * transaction.
     * @return a result Transaction
     */
    public MoneyTransaction boughtBars (int memberId, int numBars, String description)
    {
        Preconditions.checkArgument(!MemberName.isGuest(memberId), "Guests cannot buy bars.");
        Preconditions.checkArgument(numBars >= 0, "numBars is invalid: %d", numBars);

        // TODO: description should be filled in here... wtf external entity knows how
        // to make a sensible description for internal money juju?
        MoneyTransactionRecord tx = _repo.accumulateAndStoreTransaction(
            memberId, Currency.BARS, numBars, TransactionType.BARS_BOUGHT,
            description, null);
        _nodeActions.moneyUpdated(tx);

        logAction(UserAction.boughtBars(memberId), tx);

        return tx.toMoneyTransaction();
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

        // deduct from the buyer (but don't yet save the transaction)
        // (This will throw a NotEnoughMoneyException if applicable)
        MoneyTransactionRecord buyerTx = _repo.deduct(
            buyerId, buyCurrency, buyCost, buyerRec.isSupport());
        try {
            // TODO: right here: effect the purchase of the item

            // Did we give a support+ user a free item?
            boolean magicFreeItem = (buyerTx.amount == 0) && (buyCost != 0);

            // let's go ahead and insert the buyer transaction
            buyerTx.fill(
                TransactionType.ITEM_PURCHASE,
                MessageBundle.tcompose(magicFreeItem ? "m.item_magicfree" : "m.item_bought",
                    description, item.type, item.catalogId),
                item);
            _repo.storeTransaction(buyerTx);

            // see what kind of payouts we're going pay- null means don't load, don't care
            CurrencyAmount creatorPayout = magicFreeItem ? null : computePayout(false, quote);
            CurrencyAmount affiliatePayout = magicFreeItem ? null : computePayout(true, quote);

            MoneyTransactionRecord creatorTx;
            if (creatorPayout != null) {
                creatorTx = _repo.accumulateAndStoreTransaction(creatorId,
                    creatorPayout.currency, creatorPayout.amount,
                    TransactionType.CREATOR_PAYOUT,
                    MessageBundle.tcompose("m.item_sold",
                        description, item.type, item.catalogId),
                    item, buyerTx.id, buyerId);
            } else {
                creatorTx = null;
            }

            // load the buyer's affiliate
            int affiliateId = (DeploymentConfig.barsEnabled) ? buyerRec.affiliateMemberId : 0;
            MoneyTransactionRecord affiliateTx;
            if (affiliateId != 0 && affiliatePayout != null) {
                affiliateTx = _repo.accumulateAndStoreTransaction(affiliateId,
                    affiliatePayout.currency, affiliatePayout.amount,
                    TransactionType.AFFILIATE_PAYOUT,
                    MessageBundle.tcompose("m.item_affiliate", buyerRec.name, buyerRec.memberId),
                    item, buyerTx.id, buyerId);
            } else {
                affiliateTx = null;
            }

            // log this!
            logAction(UserAction.boughtItem(buyerId), buyerTx);
            if (creatorTx != null) {
                logAction(UserAction.receivedPayout(creatorId), creatorTx);
            }
            if (affiliateTx != null) {
                logAction(UserAction.receivedPayout(affiliateId), affiliateTx);
            }

            // notify affected members of their money changes
            _nodeActions.moneyUpdated(buyerTx);
            if (creatorTx != null) {
                _nodeActions.moneyUpdated(creatorTx);
            }
            if (affiliateTx != null) {
                _nodeActions.moneyUpdated(affiliateTx);
            }

            // The price no longer needs to be in the cache.
            _priceCache.removeQuote(key);
            // Inform the exchange that we've actually made the exchange
            if (!magicFreeItem) {
                // TODO: possibly pass results
                _exchange.processPurchase(quote, buyCurrency);
            }

            return new MoneyResult(
                buyerTx.toMoneyTransaction(),
                (creatorTx == null) ? null : creatorTx.toMoneyTransaction(),
                (affiliateTx == null) ? null : affiliateTx.toMoneyTransaction());

        } finally {
            // if we never inserted the buyerTrans, well that means we fucked-up, and need
            // to roll it back
            if (buyerTx.id == 0) {
                _repo.rollbackDeduction(buyerTx);
            }
        }
    }

    /**
     * Called to effect the removal of bling from a member's account for cash-out purposes.
     */
    public void cashOutBling (int memberId, int amount)
        throws NotEnoughMoneyException
    {
        MoneyTransactionRecord deductTx = _repo.deductAndStoreTransaction(
            memberId, Currency.BLING, amount * 100,
            TransactionType.CASHED_OUT, MessageBundle.tcompose("m.cashed_out", amount), null);
        // if that didn't throw a NotEnoughMoneyException, we're good to go.
        _nodeActions.moneyUpdated(deductTx);
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
        MoneyTransactionRecord deductTx = _repo.deductAndStoreTransaction(
            memberId, Currency.BLING, blingAmount * 100,
            TransactionType.SPENT_FOR_EXCHANGE,
            MessageBundle.tcompose("m.exchange_spent", blingAmount), null);
        // if that didn't throw a NotEnoughMoneyException, we're good to go.
        _nodeActions.moneyUpdated(deductTx);

        MoneyTransactionRecord accumTx = _repo.accumulateAndStoreTransaction(
            memberId, Currency.BARS, blingAmount, TransactionType.RECEIVED_FROM_EXCHANGE,
            MessageBundle.tcompose("m.exchange_added", blingAmount), null);
        _nodeActions.moneyUpdated(accumTx);
    }

    /**
     * Retrieves the amount that a member's current bling is worth in US pennies.
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
     * @param transactionTypes Set of transaction types to retrieve logs for.  If null, all
     *      transactionTypes will be retrieved.
     * @param currency Money type to retrieve logs for. If null, then records for all money types are
     * returned.
     * @param start Zero-based index of the first log item to return.
     * @param count The number of log items to return. If Integer.MAX_VALUE, this will return all
     * records.
     * @param descending If true, the log will be sorted by transaction date descending.
     * @return List of requested past transactions.
     */
    public List<MoneyTransaction> getTransactions (
        int memberId, EnumSet<TransactionType> transactionTypes, Currency currency,
        int start, int count, boolean descending, boolean support)
    {
        Preconditions.checkArgument(!MemberName.isGuest(memberId),
            "Cannot retrieve money log for guests.");
        Preconditions.checkArgument(start >= 0, "start is invalid: %d", start);
        Preconditions.checkArgument(count > 0, "count is invalid: %d", count);

        // we can't just use Lists.transform because it returns a non-serializable list
        return Lists.newArrayList(Iterables.transform(
            _repo.getTransactions(memberId, transactionTypes, currency, start, count, descending),
            support ? MoneyTransactionRecord.TO_TRANSACTION_SUPPORT
                    : MoneyTransactionRecord.TO_TRANSACTION));
    }

    /**
     * Retrieves the total number of transaction history entries we have stored for this query.
     *
     * @param memberId ID of the member to count transactions for.
     * @param transactionTypes Set of transaction types to retrieve logs for.  If null, all
     *      transactionTypes will be counted.
     * @param currency Currency to retrieve logs for. If null, then records for all money types are
     * counted.
     */
    public int getTransactionCount (
        int memberId, EnumSet<TransactionType> transactionTypes, Currency currency)
    {
        return _repo.getTransactionCount(memberId, transactionTypes, currency);
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

    /**
     * Compute an affiliate or creator payout.
     */
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

    protected void logAction (UserAction action, MoneyTransactionRecord tx)
    {
        // record this to the user action repository for later processing by the humanity helper
        _userActionRepo.logUserAction(action);

        // record this to panopticon for the greater glory of our future AI overlords
        switch (tx.currency) {
        case COINS:
            _eventLog.flowTransaction(action, tx.amount, tx.balance);
            break;

        case BARS:
            log.info("TODO: log bars to panopticon");
            break;

        case BLING:
            log.info("TODO: log bling to panopticon");
            break;
        }
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
