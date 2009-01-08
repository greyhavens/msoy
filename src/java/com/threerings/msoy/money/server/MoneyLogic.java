//
// $Id$

package com.threerings.msoy.money.server;

import java.text.NumberFormat;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.IntMap;
import com.samskivert.util.Invoker;
import com.samskivert.util.RandomUtil;
import com.samskivert.util.Tuple;

import net.sf.ehcache.CacheManager;

import com.threerings.presents.annotation.AnyThread;
import com.threerings.presents.annotation.BlockingThread;
import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.server.ShutdownManager;

import com.threerings.util.MessageBundle;

import com.threerings.messaging.MessageConnection;
import com.threerings.msoy.admin.data.MoneyConfigObject;
import com.threerings.msoy.admin.server.RuntimeConfig;
import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.server.persist.CharityRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.server.persist.UserActionRepository;

import com.threerings.msoy.item.data.all.CatalogIdent;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.server.persist.CatalogRecord;

import com.threerings.msoy.money.data.all.BlingExchangeResult;
import com.threerings.msoy.money.data.all.BlingInfo;
import com.threerings.msoy.money.data.all.CashOutBillingInfo;
import com.threerings.msoy.money.data.all.CashOutInfo;
import com.threerings.msoy.money.data.all.ExchangeData;
import com.threerings.msoy.money.data.all.ExchangeStatusData;
import com.threerings.msoy.money.data.all.MemberMoney;
import com.threerings.msoy.money.data.all.MoneyTransaction;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.PriceQuote;
import com.threerings.msoy.money.data.all.TransactionType;
import com.threerings.msoy.money.server.persist.BlingCashOutRecord;
import com.threerings.msoy.money.server.persist.ExchangeRecord;
import com.threerings.msoy.money.server.persist.MemberAccountRecord;
import com.threerings.msoy.money.server.persist.MoneyTransactionRecord;
import com.threerings.msoy.money.server.persist.MoneyRepository;

import static com.threerings.msoy.Log.log;

/**
 * Facade for all money (coins, bars, and bling) transactions. This is the starting place to
 * access these services.
 *
 * @author Kyle Sampson <kyle@threerings.net>
 * @author Ray Greenwell <ray@threerings.net>
 */
@Singleton
@BlockingThread
public class MoneyLogic
{
    /**
     * An operation to complete a purchase.
     * Right now only used in buyItem, but we may generalize that a bit to buy
     * other things.
     */
    public static interface BuyOperation
    {
        /**
         * Create the thing that is being purchased.
         * You may throw a RuntimeException or return false on failure.
         *
         * @param magicFree indicates that the product was received for free.
         * @param currency the currency used to make the purchase.
         * @param amountPaid the price paid (May be 0 even if !magicFree).
         *
         * @return true on success.
         */
        boolean create (boolean magicFree, Currency currency, int amountPaid);
    }

    @Inject
    public MoneyLogic (
        RuntimeConfig runtime, MoneyRepository repo, UserActionRepository userActionRepo,
        MsoyEventLogger eventLog, MessageConnection conn, MemberRepository memberRepo,
        ShutdownManager sm, @MainInvoker Invoker invoker, MoneyNodeActions nodeActions,
        BlingPoolDistributor blingDistributor, MoneyExchange exchange)
    {
        _runtime = runtime;
        _repo = repo;
        _userActionRepo = userActionRepo;
        _eventLog = eventLog;
        _expirer = new MoneyTransactionExpirer(repo, invoker, sm);
        _msgReceiver = new MoneyMessageListener(conn, this, memberRepo, sm, invoker);
        _nodeActions = nodeActions;
        _exchange = exchange;
        _blingDistributor = blingDistributor;
        _memberRepo = memberRepo;
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
     * Returns the current account balances (coins, bars, and bling) for the given members.
     *
     * @param memberIds IDs of the members to retrieve money for.
     * @return Map of the member ID to the money in their account.
     */
    public Map<Integer, MemberMoney> getMoneyFor (Set<Integer> memberIds)
    {
        Map<Integer, MemberMoney> monies = Maps.newHashMap();
        for (MemberAccountRecord mar : _repo.loadAll(memberIds)) {
            monies.put(mar.memberId, mar.getMemberMoney());
        }
        return monies;
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
    @AnyThread
    public void notifyCoinsEarned (int memberId, int amount)
    {
        _nodeActions.moneyUpdated(memberId, Currency.COINS, amount, true);
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
            memberId, Currency.COINS, amount, TransactionType.AWARD, action.description, null, true);
        if (notify) {
            _nodeActions.moneyUpdated(tx, true);
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
     * @param payment a String like "$2.95".
     * @return a result Transaction
     */
    public MoneyTransaction boughtBars (int memberId, int numBars, String payment)
    {
        Preconditions.checkArgument(numBars >= 0, "numBars is invalid: %d", numBars);

        UserAction action = UserAction.boughtBars(memberId, payment);
        MoneyTransactionRecord tx = _repo.accumulateAndStoreTransaction(memberId,
            Currency.BARS, numBars, TransactionType.BARS_BOUGHT, action.description, null, true);
        _nodeActions.moneyUpdated(tx, true);
        logAction(action, tx);
        return tx.toMoneyTransaction();
    }

    /**
     * The member has his purse poked by support staff. This adds or lowers their currency by
     * some amount as logged transaction.
     *
     * @param support Logged name of the acting support member.
     */
    public void supportAdjust (
        int memberId, Currency currency, int delta, MemberName support)
        throws NotEnoughMoneyException
    {
        Preconditions.checkArgument(delta <= 0, "Only deduction supported.");

        UserAction action = UserAction.supportAdjust(memberId, support);

        MoneyTransactionRecord tx = (delta > 0)
            ? _repo.accumulateAndStoreTransaction(memberId, currency, delta,
                TransactionType.SUPPORT_ADJUST, action.description, null, true)
            : _repo.deductAndStoreTransaction(memberId, currency, -delta,
                TransactionType.SUPPORT_ADJUST, action.description, null);

        _nodeActions.moneyUpdated(tx, true);
        logAction(action, tx);
    }

    /**
     * Purchases an item. This will only update the appropriate
     * accounts of an exchange of money -- item fulfillment must be handled separately.
     *
     * @param buyerRec the member record of the buying user.
     * @param catrec the catalog entry for the item, with the catalog master item loaded
     * @param buyCurrency the currency the buyer is using
     * @param authedAmount the amount the buyer has validated to purchase the item.
     * @throws NotSecuredException iff there is no secured price for the item and the authorized
     * buy amount is not enough money.
     * @return a BuyResult, or null if the BuyOperation returned false.
     */
    public BuyResult buyItem (
        final MemberRecord buyerRec, CatalogRecord catrec, Currency buyCurrency, int authedAmount,
        BuyOperation buyOp)
        throws NotEnoughMoneyException, NotSecuredException
    {
        Preconditions.checkArgument(
            buyCurrency == Currency.BARS || buyCurrency == Currency.COINS,
            "buyCurrency is invalid: %s", buyCurrency);
        Preconditions.checkArgument(catrec.item != null, "catalog master not loaded");
        CatalogIdent item = new CatalogIdent(catrec.item.getType(), catrec.catalogId);
        Preconditions.checkArgument(isValid(item), "item is invalid: %s", item);
        Currency listedCurrency = catrec.currency;
        int listedAmount = catrec.cost;
        int creatorId = catrec.item.creatorId;
        String itemName = catrec.item.name;
        ItemIdent iident = new ItemIdent(catrec.item.getType(), catrec.listedItemId);

        int buyerId = buyerRec.memberId;

        // Get the secured prices for the item.
        PriceQuote quote = _priceCache.getQuote(buyerId, item);
        if (quote == null ||
                !quote.isPurchaseValid(buyCurrency, authedAmount, _exchange.getRate())) {
            // In the unlikely scenarios that there was either no secured price (expired) or
            // they provided an out-of-date authed amount, we go ahead and secure a new price
            // right now and see if that works.
            quote = securePrice(buyerId, item, listedCurrency, listedAmount);
            if (!quote.isPurchaseValid(buyCurrency, authedAmount, _exchange.getRate())) {
                // doh, it doesn't work, so we need to tell them about this new latest price
                // we've secured for them
                throw new NotSecuredException(buyerId, item, quote);
            }
        }

        // If this buyer should always get the item for free
        boolean buyerFree = buyerRec.memberId == creatorId;

        // if the item is free, always buy in the listed currency
        if (quote.getListedAmount() == 0 || buyerFree) {
            buyCurrency = quote.getListedCurrency();
        }

        // Note that from here on, we're going to use the buyCost, which *could* be lower
        // than what the user authorized. Good for them.
        int buyCost = buyerFree ? 0 : quote.getAmount(buyCurrency);

        // deduct from the buyer (but don't yet save the transaction)
        // (This will throw a NotEnoughMoneyException if applicable)
        MoneyTransactionRecord buyerTx = _repo.deduct(
            buyerId, buyCurrency, buyCost, buyerRec.isSupport());
        try {
            // Are we giving away a free item?
            boolean magicFree = buyerFree || (buyerTx.amount == 0 && buyCost != 0);

            // actually create the item!
            boolean creationSuccess = buyOp.create(magicFree, buyerTx.currency, -buyerTx.amount);
            if (!creationSuccess) {
                return null; // stop now
            }

            // let's go ahead and insert the buyer transaction
            buyerTx.fill(
                TransactionType.ITEM_PURCHASE,
                MessageBundle.tcompose(magicFree ? "m.item_magicfree" : "m.item_bought",
                    itemName, item.type, item.catalogId),
                iident);
            _repo.storeTransaction(buyerTx);

            // If there is any change in coins from the purchase, create a transaction for it.
            MoneyTransactionRecord changeTx = null;
            if (quote.getCoinChange() > 0 && buyCurrency == Currency.BARS && !magicFree) {
                try {
                    // Don't update accumulated coins column with this.
                    changeTx = _repo.accumulateAndStoreTransaction(buyerId, Currency.COINS,
                        quote.getCoinChange(), TransactionType.CHANGE_IN_COINS,
                        MessageBundle.tcompose("m.change_received",
                            itemName, item.type, item.catalogId), iident,
                        buyerTx.id, buyerId, false);
                } catch (MoneyRepository.NoSuchMemberException nsme) {
                    // Likely a programming error in this case.
                    log.warning("Invalid original purchaser, change transaction cancelled.",
                        "buyer", buyerId, "creator", creatorId,
                        "item", itemName, "catalogIdent", item);
                    // but, we continue, just having no changeTx
                }
            }

            // see what kind of payouts we're going pay- null means don't load, don't care
            CurrencyAmount creatorPayout = magicFree ? null : computeCreatorPayout(quote);
            CurrencyAmount affiliatePayout = magicFree ? null : computeAffiliatePayout(quote);
            CurrencyAmount charityPayout = magicFree ? null : computeCharityPayout(quote);

            MoneyTransactionRecord creatorTx = null;
            if (creatorPayout != null) {
                try {
                    creatorTx = _repo.accumulateAndStoreTransaction(creatorId,
                        creatorPayout.currency, creatorPayout.amount,
                        TransactionType.CREATOR_PAYOUT,
                        MessageBundle.tcompose("m.item_sold",
                            itemName, item.type, item.catalogId),
                        iident, buyerTx.id, buyerId, true);

                } catch (MoneyRepository.NoSuchMemberException nsme) {
                    log.warning("Invalid item creator, payout cancelled.",
                        "buyer", buyerId, "creator", creatorId,
                        "item", itemName, "catalogIdent", item);
                    // but, we continue, just having no creatorTx
                }
            }

            // load the buyer's affiliate
            int affiliateId = buyerRec.affiliateMemberId;
            MoneyTransactionRecord affiliateTx = null;
            if (affiliateId != 0 && affiliatePayout != null) {
                try {
                    affiliateTx = _repo.accumulateAndStoreTransaction(affiliateId,
                        affiliatePayout.currency, affiliatePayout.amount,
                        TransactionType.AFFILIATE_PAYOUT,
                        MessageBundle.tcompose("m.item_affiliate",
                            buyerRec.name, buyerRec.memberId),
                        iident, buyerTx.id, buyerId, true);

                } catch (MoneyRepository.NoSuchMemberException nsme) {
                    log.warning("Invalid user affiliate, payout cancelled.",
                        "buyer", buyerId, "affiliate", affiliateId,
                        "item", itemName, "catalogIdent", item);
                    // but, we continue, just having no affiliateTx
                }
            }

            // Determine the ID of the charity that will receive a payout.
            int charityId = getChosenCharity(buyerRec);
            MoneyTransactionRecord charityTx = null;
            if (charityId != 0 && charityPayout != null) {
                try {
                    charityTx = _repo.accumulateAndStoreTransaction(charityId,
                        charityPayout.currency, charityPayout.amount,
                        TransactionType.CHARITY_PAYOUT,
                        MessageBundle.tcompose("m.item_charity", buyerRec.name, buyerRec.memberId),
                        iident, buyerTx.id, buyerId, true);
                } catch (MoneyRepository.NoSuchMemberException nsme) {
                    log.warning("Invalid user charity, payout cancelled.",
                        "buyer", buyerId, "charity", charityId,
                        "item", itemName, "catalogIdent", item);
                    // but, we continue, just having no charityTx
                }
            }

            // log this!
            logAction(UserAction.boughtItem(buyerId), buyerTx);
            if (changeTx != null) {
                // It's kind of a payout.  Really.
                logAction(UserAction.receivedPayout(buyerId), changeTx);
            }
            if (creatorTx != null) {
                logAction(UserAction.receivedPayout(creatorId), creatorTx);
            }
            if (affiliateTx != null) {
                logAction(UserAction.receivedPayout(affiliateId), affiliateTx);
            }
            if (charityTx != null) {
                logAction(UserAction.receivedPayout(charityId), charityTx);
            }

            // notify affected members of their money changes
            _nodeActions.moneyUpdated(buyerTx, true);
            if (changeTx != null) {
                _nodeActions.moneyUpdated(changeTx, false); // Don't accumulate
            }
            if (creatorTx != null) {
                _nodeActions.moneyUpdated(creatorTx, true);
            }
            if (affiliateTx != null) {
                _nodeActions.moneyUpdated(affiliateTx, true);
            }
            if (charityTx != null) {
                _nodeActions.moneyUpdated(charityTx, true);
            }

            // The price no longer needs to be in the cache.
            _priceCache.removeQuote(buyerId, item);
            // Inform the exchange that we've actually made the exchange
            if (!magicFree) {
                _exchange.processPurchase(quote, buyCurrency, buyerTx.id);
            }

            return new BuyResult(magicFree, buyerTx.toMoneyTransaction(),
                (changeTx == null) ? null : changeTx.toMoneyTransaction(),
                (creatorTx == null) ? null : creatorTx.toMoneyTransaction(),
                (affiliateTx == null) ? null : affiliateTx.toMoneyTransaction(),
                (charityTx == null) ? null : charityTx.toMoneyTransaction());

        } finally {
            // We may have never inserted the buyerTx if the creation failed or
            // threw a RuntimeException
            if (buyerTx.id == 0) {
                _repo.rollbackDeduction(buyerTx);
            }
        }
    }

    /**
     * Attempts to refund all money for an item by calling {@link #refundAll(Object, String)}.
     */
    public int refundAllItemPurchases (ItemIdent item, String itemName)
    {
        return refundAll(item, itemName);
    }

    /**
     * Attempts to reverse all the transactions for an item by deducting bling, bars and coins
     * earned by the creator, affiliates and charities, converting the garnished money down to
     * coins and returning them to the purchasers. The primary objective is to not generate money.
     *
     * <p>When deducting, if the deductee is out of money in one currency, then another currency is
     * attempted.</p>
     *
     * <p>The current exchange rate is used throughout. Failure occurs if the exchange rate is
     * degenerate</p>
     *
     * <p>Money absorbed by the system is magically restored, including that which would have gone
     * to an affiliate for a user with no affiliate.</p>
     *
     * @param item the subject to use for the refund transactions
     * @param itemName name to use in refund transaction description
     * @return the number of new transactions introduced
     *
     * TODO: return more information about what happened, e.g. how much money was taken from each
     * payout type and whether the account was depleted
     */
    public int refundAll (Object item, String itemName)
    {
        final int systemId = 0;
        MoneyConfigObject moneyCfg = _runtime.money;
        final float systemPct = moneyCfg.getSystemPercentage() / moneyCfg.creatorPercentage;
        final float affiliatePct = moneyCfg.affiliatePercentage / moneyCfg.creatorPercentage;
        final float xchgRate = _exchange.getRate();

        // this will cause problems for the refund, bail early
        Preconditions.checkArgument(xchgRate != 0 && xchgRate != Float.POSITIVE_INFINITY);

        log.info("Refunding purchases", "item", item, "type", item.getClass().getSimpleName(),
            "name", itemName, "exchangeRate", xchgRate);

        // function for using getOrCreate
        Function<Integer, int[]> initRefund = new Function<Integer, int[]>() {
            public int[] apply (Integer memberId) {
                return new int[Currency.values().length];
            }
        };

        // aggregate refunds per member and currency type, also resurrecting system and
        // affiliate payouts
        HashMap<Integer, int[]> refunds = Maps.newHashMap();
        HashMap<Integer, int[]> vanishedPayouts = Maps.newHashMap();
        List<Integer> affiliatedTxIds = Lists.newArrayList();
        int[] systemPurse = getOrCreate(refunds, systemId, initRefund);
        for (MoneyTransactionRecord txRec :
            _repo.getTransactionsForSubject(item, 0, Integer.MAX_VALUE, false)) {
            int currencyIdx = txRec.currency.ordinal();

            // record the earnings or spendings of this user (inverted amount) and merge spent bars
            // as a coin refund, otherwise just add into total
            if (txRec.amount < 0 && txRec.currency == Currency.BARS) {
                getOrCreate(refunds, txRec.memberId, initRefund)[Currency.COINS.ordinal()] +=
                    Math.floor(-txRec.amount * xchgRate);

            } else {
                getOrCreate(refunds, txRec.memberId, initRefund)[currencyIdx] -= txRec.amount;
            }

            // resurrect vanished money too. this will not be docked from any account but will
            // contribute to the pool
            if (txRec.transactionType == TransactionType.CREATOR_PAYOUT) {
                systemPurse[currencyIdx] -= Math.ceil(txRec.amount * systemPct);

                getOrCreate(vanishedPayouts, txRec.referenceTxId, initRefund)[currencyIdx] +=
                    Math.ceil(txRec.amount * affiliatePct);

            } else if (txRec.transactionType == TransactionType.AFFILIATE_PAYOUT) {
                affiliatedTxIds.add(txRec.referenceTxId);
            }
        }

        // remove the resurrected affiliate money if it was also paid out
        for (int txId : affiliatedTxIds)  {
            vanishedPayouts.remove(txId);
        }

        // add vanished payouts to the system purse
        for (int[] values : vanishedPayouts.values()) {
            for (int ii = values.length - 1; ii >= 0; --ii) {
                systemPurse[ii] -= values[ii];
            }
        }

        log.info("System purse", "values", systemPurse);

        // log all updates for later dispatch
        List<MoneyTransactionRecord> updates = Lists.newArrayList();

        // track how much we can give back
        float[] pool = new float[Currency.values().length];

        // apply all deductions, taking bars if the bling runs out and coins if the bars run out
        // and lastly bars if the coins run out
        Currency[] currencies = {Currency.BLING, Currency.BARS, Currency.COINS, Currency.BARS};
        for (Map.Entry<Integer, int[]> refund : refunds.entrySet()) {
            int memberId = refund.getKey();
            int[] values = refund.getValue();
            for (Currency currency : currencies) {
                int deduction = -values[currency.ordinal()];
                if (deduction <= 0) {
                    continue;
                }

                values[currency.ordinal()] = 0;

                // no account for these, just dump straight to pool
                if (memberId == systemId) {
                    pool[currency.ordinal()] += deduction;
                    continue;
                }

                // loop in case user is spending money right now and we are breaking the bank
                while (deduction > 0) {
                    try {
                        updates.add(_repo.deductAndStoreTransaction(memberId, currency, deduction,
                            TransactionType.REFUND_DEDUCTED, MessageBundle.tcompose(
                            "m.item_refunded", itemName), item));
                        pool[currency.ordinal()] += deduction;
                        deduction = 0;

                    } catch (NotEnoughMoneyException neme) {

                        int deficit = deduction - neme.getMoneyAvailable();
                        Currency conversion = null;
                        int result = 0;

                        // if they are out of bling, try bars
                        if (currency == Currency.BLING) {
                            conversion = Currency.BARS;
                            result = (int)Math.ceil(deficit / 100f);

                        // if they are out of bars, try coins
                        } else if (currency == Currency.BARS) {
                            conversion = Currency.COINS;
                            result = (int)Math.ceil(deficit * xchgRate);

                        // if they are out of coins, try bars
                        } else if (currency == Currency.COINS) {
                            conversion = Currency.BARS;
                            result = (int)Math.ceil(deficit / xchgRate);
                        }

                        if (conversion != null) {
                            values[conversion.ordinal()] -= result;
                        }

                        log.info("Account ran out of money during refund deductions phase",
                            "memberId", memberId, "currency", currency, "deficit", deficit,
                            "conversion", conversion, "result", result);

                        // take what we can next time around
                        deduction = neme.getMoneyAvailable();
                    }
                }
            }
        }

        log.info("Initial refund pool", "amounts", pool);

        // convert everything to coins
        pool[Currency.BARS.ordinal()] += pool[Currency.BLING.ordinal()] / 100f;
        pool[Currency.COINS.ordinal()] += Math.floor(pool[Currency.BARS.ordinal()] * xchgRate);

        log.info("Converted refund pool", "amounts", pool);

        // now disperse the coins
        // TODO: distribute the coins evenly if there are not enough
        int coinPool = (int)pool[Currency.COINS.ordinal()];
        for (Map.Entry<Integer, int[]> refund : refunds.entrySet()) {
            int memberId = refund.getKey();
            int[] values = refund.getValue();
            int refundAmount = values[Currency.COINS.ordinal()];
            if (refundAmount > 0 && (values[Currency.BARS.ordinal()] != 0 ||
                values[Currency.BLING.ordinal()] != 0)) {
                log.warning("Issuing coin refund, but bars and bling have non-zero balance",
                    "refund", values, "memberId", memberId);
            }

            if (refundAmount > coinPool) {
                log.info("Issuing reduced refund due to insufficient funds",
                    "memberId", memberId, "desiredAmount", refundAmount, "coinPool", coinPool);
                refundAmount = coinPool;
            }

            if (refundAmount <= 0) {
                continue;
            }

            updates.add(_repo.accumulateAndStoreTransaction(memberId, Currency.COINS, refundAmount,
                TransactionType.REFUND_GIVEN, MessageBundle.tcompose("m.item_refund",
                itemName), item, false));
            coinPool -= refundAmount;
        }

        for (MoneyTransactionRecord txRec : updates) {
            // TODO: logAction?
            // notify members
            _nodeActions.moneyUpdated(txRec, true);
        }

        return updates.size();
    }

    /**
     * Called to effect the removal of bling from a member's account for cash-out purposes.
     *
     * @param memberId ID of the member whose bling will be cashed out.
     * @param amount Amount of the centibling to actually cash out.  This may be different than the
     * amount requested, at the discretion of the support person handling this request.
     */
    public void cashOutBling (int memberId, int amount)
        throws NotEnoughMoneyException
    {
        BlingCashOutRecord cashOut = _repo.getCurrentCashOutRequest(memberId);
        if (cashOut == null) {
            return; // No effect if this member has no pending cashout
        }
        String payment = formatUSD(amount * cashOut.blingWorth / 100);
        MoneyTransactionRecord deductTx = _repo.deductAndStoreTransaction(
            memberId, Currency.BLING, amount,
            TransactionType.CASHED_OUT, MessageBundle.tcompose("m.cashed_out", payment), null);
        _repo.commitBlingCashOutRequest(memberId, amount);

        // if that didn't throw a NotEnoughMoneyException, we're good to go.
        _nodeActions.moneyUpdated(deductTx, true);
        logAction(UserAction.cashedOutBling(memberId), deductTx);
    }

    /**
     * Cancels a request to cash out bling.  This is done by support / admins only when there are
     * cases when they cannot cash out or suspect illicit activity.
     *
     * @param memberId The member whose request should be canceled.
     * @param reason A reason for canceling the request, to be included on the transaction log for
     * the user.
     */
    public void cancelCashOutBling (int memberId, String reason)
    {
        _repo.cancelBlingCashOutRequest(memberId, reason);
    }

    /**
     * Requests a bling cash out for a particular user.  This will not immediately deduct any bling.
     * The user must have the specified amount of bling currently in their account, and they must
     * not already be waiting for a cash out request to be fulfilled.
     *
     * @param memberId ID of the member making the request.
     * @param amount Amount of bling (NOT centibling) to cash out.
     * @throws NotEnoughMoneyException The user does not currently have the amount of bling in their
     * account.
     * @throws AlreadyCashedOutException The user has already requested a bling cash out that has
     * not yet been fulfilled.
     * @throws BelowMinimumBlingException The amount requested is below the minimum amount allowed
     * for cashing out.
     */
    public BlingInfo requestCashOutBling (int memberId, int amount, CashOutBillingInfo info)
        throws NotEnoughMoneyException, AlreadyCashedOutException, BelowMinimumBlingException
    {
        MemberAccountRecord account = _repo.load(memberId);

        // If the user does not have the minimum amount required to cash out bling, don't allow
        // them to proceed
        if (amount < _runtime.money.minimumBlingCashOut) {
            throw new BelowMinimumBlingException(
                memberId, amount, _runtime.money.minimumBlingCashOut);
        }

        // Ensure the account has the requested amount of bling and that it is currently not
        // requesting a cash out.
        int blingAmount = amount * 100;
        if (account.bling < blingAmount) {
            throw new NotEnoughMoneyException(memberId, Currency.BLING, blingAmount, account.bling);
        }
        if (_repo.getCurrentCashOutRequest(memberId) != null) {
            throw new AlreadyCashedOutException(memberId, account.cashOutBling);
        }

        // Add a cash out record for this member.
        BlingCashOutRecord cashOut = _repo.createCashOut(
            memberId, blingAmount, _runtime.money.blingWorth, info);

        return TO_BLING_INFO.apply(new Tuple<MemberAccountRecord, BlingCashOutRecord>(
                account, cashOut));
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
    public BlingExchangeResult exchangeBlingForBars (int memberId, int blingAmount)
        throws NotEnoughMoneyException
    {
        MoneyTransactionRecord deductTx = _repo.deductAndStoreTransaction(
            memberId, Currency.BLING, blingAmount * 100,
            TransactionType.SPENT_FOR_EXCHANGE, "m.exchanged_for_bars", null);
        // if that didn't throw a NotEnoughMoneyException, we're good to go.
        _nodeActions.moneyUpdated(deductTx, true);

        MoneyTransactionRecord accumTx = _repo.accumulateAndStoreTransaction(
            memberId, Currency.BARS, blingAmount,
            TransactionType.RECEIVED_FROM_EXCHANGE, "m.exchanged_from_bling", null, true);
        _nodeActions.moneyUpdated(accumTx, true);

        logAction(UserAction.exchangedCurrency(memberId), deductTx);
        logAction(UserAction.exchangedCurrency(memberId), accumTx);

        return new BlingExchangeResult(accumTx.balance, getBlingInfo(memberId));
    }

    /**
     * Retrieves information about a member's bling, including its current worth and whether or
     * not they have a pending request to cash out bling.
     */
    public BlingInfo getBlingInfo (int memberId)
    {
        return TO_BLING_INFO.apply(new Tuple<MemberAccountRecord, BlingCashOutRecord>(
                _repo.load(memberId), _repo.getCurrentCashOutRequest(memberId)));
    }

    /**
     * Finds all members currently requesting bling cash outs and returns bling information for
     * each of them.
     */
    public Map<Integer, CashOutInfo> getBlingCashOutRequests ()
    {
        List<BlingCashOutRecord> accounts = _repo.getAccountsCashingOut();

        // First transform List<CashOutRecord> into Map<Integer, CashOutRecord>, then
        // transform into Map<Integer, CashOutInfo>.
        return transformMap(Maps.uniqueIndex(accounts, new Function<BlingCashOutRecord, Integer>() {
            public Integer apply (BlingCashOutRecord record) {
                return record.memberId;
            }
        }), TO_CASH_OUT_INFO);
    }

    /**
     * Retrieves a money transaction history for a member, including one or more money types. The
     * returned list is sorted by transaction date ascending. A portion of the log can be returned
     * at a time for pagination.
     *
     * @param memberId ID of the member to retrieve money for.
     * @param transactionTypes Set of transaction types to retrieve logs for.  If null, all
     *      transactionTypes will be retrieved.
     * @param currency Money type to retrieve logs for. If null, then records for all types are
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
        List<MoneyTransaction> txList = Lists.newArrayList(Iterables.transform(
            _repo.getTransactions(memberId, transactionTypes, currency, start, count, descending),
            support ? MoneyTransactionRecord.TO_TRANSACTION_SUPPORT
                    : MoneyTransactionRecord.TO_TRANSACTION));

        if (support) {
            fillInMemberNames(txList);
        }
        return txList;
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
     * Loads all transactions that were inserted with the given subject.
     */
    public List<MoneyTransaction> getItemTransactions (
        ItemIdent item, int from, int count, boolean descending)
    {
        List<MoneyTransaction> txList = Lists.newArrayList(Iterables.transform(
            _repo.getTransactionsForSubject(item, from, count, descending),
            MoneyTransactionRecord.TO_TRANSACTION_SUPPORT));
        fillInMemberNames(txList);
        return txList;
    }

    /**
     * Counts the number of transactions that were inserted with the given subject.
     */
    public int getItemTransactionCount (ItemIdent item)
    {
        return _repo.getTransactionCountForSubject(item);
    }

    /**
     * Get current exchange status.
     */
    public ExchangeStatusData getExchangeStatus (int start, int count)
    {
        List<ExchangeData> page = Lists.newArrayList(Iterables.transform(
            _repo.getExchangeData(start, count), ExchangeRecord.TO_EXCHANGE_DATA));
        int total = _repo.getExchangeDataCount();
        int[] barPoolData = _repo.getBarPool(_runtime.money.barPoolSize);

        return new ExchangeStatusData(total, page,
            _exchange.getRate(), _runtime.money.targetExchangeRate,
            barPoolData[0], _runtime.money.barPoolSize, barPoolData[1]);
    }

    /**
     * Secures a price for an item. This ensures the user will be able to purchase an item
     * for a set price. This price will remain available for some amount of time (specified by
     * {@link PriceQuoteCache#SECURED_PRICE_DURATION}. The secured price may also be removed
     * if the maximum number of secured prices system-wide has been reached (specified by
     * {@link PriceQuoteCache#MAX_SECURED_PRICES}. In either case, an attempt to buy the
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
            _priceCache.addQuote(buyerId, item, quote);
        }
        return quote;
    }

    /**
     * Initializes the money service by starting up required services, such as an expiration
     * monitor and queue listeners. This method is idempotent.
     */
    public void init (CacheManager cacheMgr)
    {
        _priceCache.init(cacheMgr);
        _exchange.init();
        _msgReceiver.start();
        _blingDistributor.start();
    }

    /**
     * Is this CatalogIdent valid?
     */
    protected static boolean isValid (CatalogIdent ident)
    {
        return (ident != null) && (ident.type != Item.NOT_A_TYPE) && (ident.catalogId != 0);
    }

    /**
     * Compute the payout that the creator should receive for the given price quote.
     */
    protected CurrencyAmount computeCreatorPayout (PriceQuote quote)
    {
        return computePayout(quote, _runtime.money.creatorPercentage);
    }

    /**
     * Compute the payout that the affiliate should receive for the given price quote.  There
     * should be no payout transaction if the amount is 0.
     */
    protected CurrencyAmount computeAffiliatePayout (PriceQuote quote)
    {
        CurrencyAmount ca = computePayout(quote, _runtime.money.affiliatePercentage);

        // for creators, we pay out "0" so that they get a sales report,
        // but we never do that for affiliates
        return (ca.amount == 0 ? null : ca);
    }

    /**
     * Compute the payout that the charity should receive for the given price quote.
     */
    protected CurrencyAmount computeCharityPayout (PriceQuote quote)
    {
        CurrencyAmount ca = computePayout(quote, _runtime.money.charityPercentage);

        // Like affiliates, if the payout amount is 0, return null to avoid creating a tx.
        return (ca.amount == 0 ? null : ca);
    }

    /**
     * Compute a payout with the given percentage.
     */
    protected CurrencyAmount computePayout (PriceQuote quote, float percentage)
    {
        Currency currency;
        int amount;

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

        return new CurrencyAmount(currency, amount);
    }

    protected void logAction (UserAction action, MoneyTransactionRecord tx)
    {
        // record this to the user action repository for later processing by the humanity helper
        _userActionRepo.logUserAction(action);

        // record this to panopticon for the greater glory of our future AI overlords
        _eventLog.moneyTransaction(action, tx.currency, tx.amount);
    }

    /**
     * Selects the charity that will be used for this purchase.  If the member has chosen a
     * specific charity, it will be used.  Otherwise, a random core charity will be selected.
     * If there are no charities available, returns 0.
     */
    protected int getChosenCharity (MemberRecord member)
    {
        // If the user has selected a specific charity, use it.
        if (member.charityMemberId != 0) {
            return member.charityMemberId;
        }

        // Otherwise, we must select a random core charity.  This is a fast query, but it is a DB
        // trip, so perhaps some optimization could be used here.  However, compared to the writes
        // that have to be done at the same time, this is somewhat trivial.
        List<CharityRecord> charities = _memberRepo.getCoreCharities();
        if (charities.isEmpty()) {
            return 0;
        } else {
            return RandomUtil.pickRandom(charities).memberId;
        }
    }

    protected void fillInMemberNames (List<MoneyTransaction> txList)
    {
        Set<Integer> memberIds = new HashSet<Integer>();
        for (MoneyTransaction tx : txList) {
            memberIds.add(tx.referenceMemberName.getMemberId());
        }
        IntMap<MemberName> names = _memberRepo.loadMemberNames(memberIds);
        for (MoneyTransaction tx : txList) {
            tx.referenceMemberName = names.get(tx.referenceMemberName.getMemberId());
        }
    }

    /**
     * Converts the amount of pennies into a string to display to the user as a valid currency.
     * Note: there are some other utilities around to do this, but they're either in a different
     * project (and there's some concern about exposing them directly), or they don't properly
     * take into account floating-point round off errors.  This may get replaced or expanded
     * later on.
     */
    protected static String formatUSD (int pennies)
    {
        int dollars = pennies / 100;
        int cents = pennies % 100;
        return "USD $" + NumberFormat.getNumberInstance().format(dollars) + '.' +
            (cents < 10 ? '0' : "") + cents;
    }

    /**
     * Transforms the values in a map from one type to another, as dictated by the given function.
     * I'm surprised this isn't currently in Google collections...
     *
     * @param <K> Type of the map's key.
     * @param <V1> Type of the source values.
     * @param <V2> Type of the destination values.
     * @param values Map of the values to transform.
     * @param function Function to do the transformation.
     * @return The transformed map.
     */
    protected static <K, V1, V2> Map<K, V2> transformMap (
        Map<K, V1> values, Function<? super V1, ? extends V2> function)
    {
        Map<K, V2> newMap = Maps.newHashMap();
        for (Map.Entry<K, V1> value : values.entrySet()) {
            newMap.put(value.getKey(), function.apply(value.getValue()));
        }
        return newMap;
    }

    /**
     * Avoids the repetitive pattern of getting an entry from a map and putting a new (blank) one
     * if it is null.
     */
    protected static <K, V> V getOrCreate (Map<K, V> map, K key, Function <K, V> createFunc)
    {
        V value = map.get(key);
        if (value == null) {
            map.put(key, value = createFunc.apply(key));
        }
        return value;
    }

    /** A Function that transforms a MemberArroundRecord and CashOutRecord to BlingInfo. */
    protected final Function<Tuple<MemberAccountRecord, BlingCashOutRecord>, BlingInfo>
        TO_BLING_INFO =
        new Function<Tuple<MemberAccountRecord, BlingCashOutRecord>, BlingInfo>() {
            public BlingInfo apply (Tuple<MemberAccountRecord, BlingCashOutRecord> records) {
                return new BlingInfo(records.left.bling, _runtime.money.blingWorth,
                    _runtime.money.minimumBlingCashOut * 100,
                    records.right == null ? null : records.right.toInfo());
            }
        };

    /** A Function that transforms a CashOutRecord into a CashOutInfo. */
    protected final Function<BlingCashOutRecord, CashOutInfo> TO_CASH_OUT_INFO =
        new Function<BlingCashOutRecord, CashOutInfo>() {
            public CashOutInfo apply (BlingCashOutRecord record) {
                return record.toInfo();
            }
        };

    protected final RuntimeConfig _runtime;
    protected final MoneyExchange _exchange;
    protected final MoneyTransactionExpirer _expirer;
    protected final MsoyEventLogger _eventLog;
    protected final UserActionRepository _userActionRepo;
    protected final MoneyRepository _repo;
    protected final PriceQuoteCache _priceCache = new PriceQuoteCache();
    protected final MoneyMessageListener _msgReceiver;
    protected final MoneyNodeActions _nodeActions;
    protected final BlingPoolDistributor _blingDistributor;
    protected final MemberRepository _memberRepo;
}
