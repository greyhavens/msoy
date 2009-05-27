//
// $Id$

package com.threerings.msoy.money.server;

import java.text.NumberFormat;
import java.util.Collections;
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

import com.samskivert.depot.DatabaseException;
import com.samskivert.util.IntMap;
import com.samskivert.util.RandomUtil;

import net.sf.ehcache.CacheManager;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.util.MessageBundle;

import com.threerings.msoy.admin.data.MoneyConfigObject;
import com.threerings.msoy.admin.server.RuntimeConfig;
import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.server.persist.CharityRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.server.persist.UserActionRepository;
import com.threerings.msoy.web.gwt.ServiceException;

import com.threerings.msoy.item.data.all.CatalogIdent;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.server.persist.CatalogRecord;

import com.threerings.msoy.money.data.MoneyCodes;
import com.threerings.msoy.money.data.all.BlingExchangeResult;
import com.threerings.msoy.money.data.all.BlingInfo;
import com.threerings.msoy.money.data.all.CashOutBillingInfo;
import com.threerings.msoy.money.data.all.CashOutInfo;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.ExchangeData;
import com.threerings.msoy.money.data.all.ExchangeStatusData;
import com.threerings.msoy.money.data.all.MemberMoney;
import com.threerings.msoy.money.data.all.MoneyTransaction;
import com.threerings.msoy.money.data.all.PriceQuote;
import com.threerings.msoy.money.data.all.TransactionType;
import com.threerings.msoy.money.gwt.CostUpdatedException;
import com.threerings.msoy.money.gwt.InsufficientFundsException;
import com.threerings.msoy.money.server.persist.BlingCashOutRecord;
import com.threerings.msoy.money.server.persist.ExchangeRecord;
import com.threerings.msoy.money.server.persist.MemberAccountRecord;
import com.threerings.msoy.money.server.persist.MoneyRepository;
import com.threerings.msoy.money.server.persist.MoneyTransactionRecord;

import static com.threerings.msoy.Log.log;

/**
 * Facade for all money (coins, bars, and bling) transactions. This is the starting place to
 * access these services.
 *
 * @author Kyle Sampson <kyle@threerings.net>
 * @author Ray Greenwell <ray@threerings.net>
 */
@Singleton @BlockingThread
public class MoneyLogic
{
    /**
     * An operation to complete a purchase.
     */
    public static abstract class BuyOperation<T>
    {
        public static final BuyOperation<Void> NOOP = new BuyOperation<Void>() {
            public Void create (boolean magicFree, Currency currency, int amountPaid) {
                return null;
            }
        };

        /**
         * Create the thing that is being purchased. You may throw a ServiceException or
         * RuntimeException to indicate failure.
         *
         * @param magicFree indicates that the product was received for free.
         * @param currency the currency used to make the purchase.
         * @param amountPaid the price paid (May be 0 even if !magicFree).
         *
         * @return the ware purchased by buy operation.
         */
        public abstract T create (boolean magicFree, Currency currency, int amountPaid)
            throws ServiceException;
    }

    /**
     * Initializes the money service by starting up required services, such as an expiration
     * monitor and queue listeners. This method is idempotent.
     */
    public void init (CacheManager cacheMgr)
    {
        _priceCache.init(cacheMgr);
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
        Preconditions.checkArgument(memberId != 0, "Requested money for invalid member.");
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
     * Shorthand for calling {@link #award} with <code>Currency.COINS</code> and
     * <code>TransactionType.AWARD</code>.
     */
    public MoneyTransaction awardCoins (int memberId, int amount, boolean notify, UserAction action)
    {
        return award(memberId, Currency.COINS, TransactionType.AWARD, amount, notify, action);
    }

    /**
     * Awards some number of coins or bars to a member for some activity, such as playing a game.
     *
     * @param memberId ID of the member to receive the coins.
     * @param currency Currency being awarded
     * @param type Type of transaction that this is for
     * @param amount Number of coins to be awarded.
     * @param notify If false, an earlier call to {@link MoneyNodeActions#coinsEarned} was made, so
     * this call should not notify the user.
     * @param action The user action that caused coins to be awarded.
     */
    public MoneyTransaction award (int memberId, Currency currency, TransactionType type,
                                   int amount, boolean notify, UserAction action)
    {
        Preconditions.checkArgument(memberId != 0, "Requested to award money to invalid member.");
        Preconditions.checkArgument(amount >= 0, "amount is invalid: %d", amount);
        MoneyTransactionRecord tx = _repo.accumulateAndStoreTransaction(
            memberId, currency, amount, type, action.description, null, true);
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
     * The member has his purse poked by support staff. This adds or lowers their currency by some
     * amount as logged transaction.
     *
     * @param support Logged name of the acting support member.
     */
    public void supportAdjust (int memberId, Currency currency, int delta, MemberName support)
        throws ServiceException
    {
        Preconditions.checkArgument(delta <= 0, "Only deduction supported.");

        UserAction action = UserAction.supportAdjust(memberId, support);
        MoneyTransactionRecord tx;
        if (delta > 0) {
            tx = _repo.accumulateAndStoreTransaction(
                memberId, currency, delta, TransactionType.SUPPORT_ADJUST, action.description,
                null, true);
        } else {
            tx = _repo.deductAndStoreTransaction(
                memberId, currency, -delta, TransactionType.SUPPORT_ADJUST, action.description,
                null);
        }
        _nodeActions.moneyUpdated(tx, true);
        logAction(action, tx);
    }

    /**
     * Purchases an item. This will only update the appropriate
     * accounts of an exchange of money -- item fulfillment must be handled separately.
     *
     * @param buyerRec the member record of the buying user.
     * @param listings the catalog entries for the item, with the catalog master items loaded. The
     * first item is the item being purchased. subsequent items are bases
     * @param buyCurrency the currency the buyer is using
     * @param authedAmount the amount the buyer has validated to purchase the item.
     *
     * @return the results of the purchase (never null).
     *
     * @throws CostUpdatedException iff there is no secured price for the item and the authorized
     * buy amount is not enough money.
     */
    public <T> BuyResult<T> buyItem (final MemberRecord buyerRec, List<CatalogRecord> listings,
                                     Currency buyCurrency, int authedAmount, BuyOperation<T> buyOp)
        throws ServiceException
    {
        CatalogRecord catrec = listings.get(0);
        Preconditions.checkArgument(catrec.item != null, "catalog master not loaded");
        final CatalogIdent item = new CatalogIdent(catrec.item.getType(), catrec.catalogId);
        Preconditions.checkArgument(isValid(item), "item is invalid: %s", item);
        int buyerId = buyerRec.memberId;
        final String itemName = catrec.item.name;
        ItemIdent iident = new ItemIdent(catrec.item.getType(), catrec.listedItemId);
        boolean forceFree = (buyerId == catrec.item.creatorId);
        Function<Boolean, String> buyMsgFn = new Function<Boolean,String>() {
            public String apply (Boolean magicFree) {
                return MessageBundle.tcompose(magicFree ? "m.item_magicfree" : "m.item_bought",
                    itemName, item.type, item.catalogId);
            }
        };
        String changeMsg = MessageBundle.tcompose("m.change_received",
            itemName, item.type, item.catalogId);

        // do the buy!
        IntermediateBuyResult<T> ibr = buy(buyerRec, item, buyCurrency, authedAmount,
            forceFree, catrec.currency, catrec.cost, buyOp, TransactionType.ITEM_PURCHASE,
            buyMsgFn, iident, changeMsg);
        if (ibr == null) {
            return null;
        }

        // now process the results of the buy
        boolean magicFree = ibr.magicFree;
        PriceQuote quote = ibr.quote;
        MoneyTransactionRecord buyerTx = ibr.buyerTx;
        MoneyTransactionRecord changeTx = ibr.changeTx;

        // see what kind of payouts we're going pay- null means don't load, don't care
        List<CurrencyAmount> creatorPayouts =
            magicFree ? null : computeCreatorPayouts(quote, listings);
        CurrencyAmount affiliatePayout = magicFree ? null : computeAffiliatePayout(quote);
        CurrencyAmount charityPayout = magicFree ? null : computeCharityPayout(quote);

        List<MoneyTransactionRecord> creatorTxs = null;
        if (creatorPayouts != null) {
            creatorTxs = Lists.newArrayListWithExpectedSize(creatorPayouts.size());
            for (int ii = 0; ii < creatorPayouts.size(); ++ii) {
                int creatorId = listings.get(ii).item.creatorId;
                TransactionType txType = ii == 0 ? TransactionType.CREATOR_PAYOUT :
                    TransactionType.BASIS_CREATOR_PAYOUT;
                CurrencyAmount amount = creatorPayouts.get(ii);
                String message = MessageBundle.tcompose(ii == 0 ? "m.item_sold" :
                    "m.derived_item_sold", itemName, item.type, item.catalogId);
                try {
                    creatorTxs.add(_repo.accumulateAndStoreTransaction(
                                       creatorId, amount.currency, amount.amount, txType, message,
                                       iident, buyerTx.id, buyerId, true));
                } catch (DatabaseException de) {
                    log.warning(de.getMessage()); // keep going with the main transaction
                }
            }
        }

        // load the buyer's affiliate
        int affiliateId = buyerRec.affiliateMemberId;
        MoneyTransactionRecord affiliateTx = null;
        if (affiliateId != 0 && affiliatePayout != null) {
            try {
                affiliateTx = _repo.accumulateAndStoreTransaction(
                    affiliateId, affiliatePayout.currency, affiliatePayout.amount,
                    TransactionType.AFFILIATE_PAYOUT,
                    MessageBundle.tcompose("m.item_affiliate", buyerRec.name, buyerRec.memberId),
                    iident, buyerTx.id, buyerId, true);
            } catch (DatabaseException de) {
                log.warning(de.getMessage()); // keep going with the main transaction
            }
        }

        // Determine the ID of the charity that will receive a payout.
        int charityId = getChosenCharity(buyerRec);
        MoneyTransactionRecord charityTx = null;
        if (charityId != 0 && charityPayout != null) {
            try {
                charityTx = _repo.accumulateAndStoreTransaction(
                    charityId, charityPayout.currency, charityPayout.amount,
                    TransactionType.CHARITY_PAYOUT,
                    MessageBundle.tcompose("m.item_charity", buyerRec.name, buyerRec.memberId),
                    iident, buyerTx.id, buyerId, true);
            } catch (DatabaseException de) {
                log.warning(de.getMessage()); // keep going with the main transaction
            }
        }

        // log this!
        logAction(UserAction.boughtItem(buyerId), buyerTx);
        if (changeTx != null) {
            // It's kind of a payout.  Really.
            logAction(UserAction.receivedPayout(buyerId), changeTx);
        }
        if (creatorTxs != null) {
            for (MoneyTransactionRecord tx : creatorTxs) {
                logAction(UserAction.receivedPayout(tx.memberId), tx);
            }
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
        if (creatorTxs != null) {
            for (MoneyTransactionRecord tx : creatorTxs) {
                _nodeActions.moneyUpdated(tx, true);
            }
        }
        if (affiliateTx != null) {
            _nodeActions.moneyUpdated(affiliateTx, true);
        }
        if (charityTx != null) {
            _nodeActions.moneyUpdated(charityTx, true);
        }

        return new BuyResult<T>(magicFree, buyerTx.toMoneyTransaction(),
                                (changeTx == null) ? null : changeTx.toMoneyTransaction(),
                                (creatorTxs == null) ? null : Lists.transform(
                                    creatorTxs, MoneyTransactionRecord.TO_TRANSACTION),
                                (affiliateTx == null) ? null : affiliateTx.toMoneyTransaction(),
                                (charityTx == null) ? null : charityTx.toMoneyTransaction(),
                                ibr.ware);
    }

    /**
     * Process the purchase of a party.
     */
    public BuyResult<Void> buyParty (
        int buyerId, Object partyKey, Currency buyCurrency, int authedAmount,
        Currency listCurrency, int listAmount)
        throws ServiceException
    {
        MemberRecord buyerRec = _memberRepo.loadMember(buyerId);
        return buyFromOOO(
            buyerRec, partyKey, buyCurrency, authedAmount, listCurrency, listAmount,
            BuyOperation.NOOP, UserAction.Type.BOUGHT_PARTY, "m.party_bought",
            TransactionType.PARTY_PURCHASE, "m.change_rcvd_party");
    }

    /**
     * Process the purchase of a room.
     */
    public <T> BuyResult<T> buyRoom (
        MemberRecord buyerRec, Object roomKey, Currency buyCurrency, int authedAmount,
        Currency listCurrency, int listAmount, BuyOperation<T> buyOp)
        throws ServiceException
    {
        return buyFromOOO(
            buyerRec, roomKey, buyCurrency, authedAmount, listCurrency, listAmount,
            buyOp, UserAction.Type.BOUGHT_ROOM, "m.room_bought", TransactionType.ROOM_PURCHASE,
            "m.change_rcvd_room");
    }

    /**
     * Process the purchase of a group.
     */
    public <T> BuyResult<T> buyGroup (
        MemberRecord buyerRec, Object groupKey, Currency buyCurrency, int authedAmount,
        Currency listCurrency, int listAmount, String groupName, BuyOperation<T> buyOp)
        throws ServiceException
    {
        return buyFromOOO(
            buyerRec, groupKey, buyCurrency, authedAmount, listCurrency, listAmount, buyOp,
            UserAction.Type.BOUGHT_GROUP, MessageBundle.tcompose("m.group_created", groupName),
            TransactionType.GROUP_PURCHASE, "m.change_rcvd_group");
    }

    /**
     * Processes the fee charged when listing an item.
     */
    public <T> BuyResult<T> listItem (
        MemberRecord listerRec, int listFee, String itemName, BuyOperation<T> buyOp)
        throws ServiceException
    {
        return buyFromOOO(
            listerRec, LIST_ITEM_KEY, Currency.COINS, listFee, Currency.COINS, listFee, buyOp,
            UserAction.Type.LISTED_ITEM, MessageBundle.tcompose("m.created_listing", itemName),
            TransactionType.CREATED_LISTING, null /* never any change */);
    }

    /**
     * Process a purchase of a ware from Three Rings.
     */
    public <T> BuyResult<T> buyFromOOO (
        MemberRecord buyerRec, Object wareKey, Currency buyCurrency, int authedAmount,
        Currency listCurrency, int listAmount, BuyOperation<T> buyOp, UserAction.Type buyActionType,
        final String boughtTxMsg, TransactionType boughtTxType, String changeTxMsg)
        throws ServiceException
    {
        int buyerId = buyerRec.memberId;
        Function<Boolean, String> boughtTxMsgFn = new Function<Boolean,String>() {
            public String apply (Boolean magicFree) {
                return boughtTxMsg;
            }
        };

        // do the buy!
        IntermediateBuyResult<T> ibr = buy(
            buyerRec, wareKey, buyCurrency, authedAmount, false /*forcefree*/,
            listCurrency, listAmount, buyOp, boughtTxType, boughtTxMsgFn,
            null /*subject*/, changeTxMsg);
        if (ibr == null) {
            return null;
        }

        // now process the results of the buy
        MoneyTransactionRecord buyerTx = ibr.buyerTx;
        MoneyTransactionRecord changeTx = ibr.changeTx;

        // log this!
        logAction(UserAction.boughtFromOOO(buyerId, buyActionType, boughtTxMsg), buyerTx);
        if (changeTx != null) {
            // It's kind of a payout.  Really.
            logAction(UserAction.receivedPayout(buyerId), changeTx);
        }

        // notify affected members of their money changes
        _nodeActions.moneyUpdated(buyerTx, true);
        if (changeTx != null) {
            _nodeActions.moneyUpdated(changeTx, false); // Don't accumulate
        }

        return new BuyResult<T>(ibr.magicFree, buyerTx.toMoneyTransaction(),
                                (changeTx == null) ? null : changeTx.toMoneyTransaction(),
                                null, null, null, ibr.ware);
    }

    /**
     * Purchases SOMETHING. This will only update the appropriate accounts of an exchange of money
     * -- item fulfillment must be handled separately.
     *
     * @param buyerRec the member record of the buying user.
     * @param wareKey the key identifying the ware being sold.
     * @param buyCurrency the currency the buyer is using
     * @param authedAmount the amount the buyer has validated to purchase the item.
     * @param forceFree force the transaction to be free
     * @param listedCurrency the currency in which the payee will be paid
     * @param listedAmount the amount the payee will be paid
     * @param buyOp enacts the purchase
     *
     * @return a BuyResult, or null if the BuyOperation returned false.
     *
     * @throws CostUpdatedException iff there is no secured price for the item and the authorized
     * buy amount is not enough money.
     */
    public <T> IntermediateBuyResult<T> buy (
        MemberRecord buyerRec, Object wareKey, Currency buyCurrency, int authedAmount,
        boolean forceFree, Currency listedCurrency, int listedAmount, BuyOperation<T> buyOp,
        TransactionType buyerTxType, Function<Boolean,String> buyMsgFn, Object subject,
        String changeMsg)
        throws ServiceException
    {
        Preconditions.checkArgument(
            buyCurrency == Currency.BARS || buyCurrency == Currency.COINS,
            "buyCurrency is invalid: %s", buyCurrency);

        // Get the secured prices for the item.
        int buyerId = buyerRec.memberId;
        PriceQuote quote = _priceCache.getQuote(buyerId, wareKey);
        if (quote == null ||
                !quote.isPurchaseValid(buyCurrency, authedAmount, _exchange.getRate())) {
            // In the unlikely scenarios that there was either no secured price (expired) or
            // they provided an out-of-date authed amount, we go ahead and secure a new price
            // right now and see if that works.
            quote = securePrice(buyerId, wareKey, listedCurrency, listedAmount);
            if (!quote.isPurchaseValid(buyCurrency, authedAmount, _exchange.getRate())) {
                // doh, it doesn't work, so we need to tell them about this new latest price we've
                // secured for them
                throw new CostUpdatedException(quote);
            }
        }

        // if the item is free, always buy in the listed currency
        if (forceFree || quote.getListedAmount() == 0) {
            buyCurrency = quote.getListedCurrency();
        }

        // Note that from here on, we're going to use the buyCost, which *could* be lower
        // than what the user authorized. Good for them.
        int buyCost = forceFree ? 0 : quote.getAmount(buyCurrency);

        // deduct from the buyer (but don't yet save the transaction)
        MoneyTransactionRecord buyerTx =
            _repo.deduct(buyerId, buyCurrency, buyCost, buyerRec.isSupport());
        try {
            // Are we giving away a free item?
            boolean magicFree = forceFree || (buyerTx.amount == 0 && buyCost != 0);

            // actually create the item!
            T ware = buyOp.create(magicFree, buyerTx.currency, -buyerTx.amount);

            // go ahead and insert the buyer transaction
            buyerTx.fill(buyerTxType, buyMsgFn.apply(magicFree), subject);
            _repo.storeTransaction(buyerTx);

            // The price no longer needs to be in the cache.
            _priceCache.removeQuote(buyerId, wareKey);
            // Inform the exchange that we've actually made the exchange
            if (!magicFree) {
                _exchange.processPurchase(quote, buyCurrency, buyerTx.id);
            }

            // If there is any change in coins from the purchase, create a transaction for it.
            MoneyTransactionRecord changeTx = null;
            if (!magicFree && (buyCurrency == Currency.BARS) && (quote.getCoinChange() > 0)) {
                // Don't update accumulated coins column with this.
                changeTx = _repo.accumulateAndStoreTransaction(
                    buyerId, Currency.COINS, quote.getCoinChange(), TransactionType.CHANGE_IN_COINS,
                    changeMsg, subject, buyerTx.id, buyerId, false);
            }

            return new IntermediateBuyResult<T>(magicFree, quote, buyerTx, changeTx, ware);

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

                    } catch (InsufficientFundsException nsf) {
                        int deficit = deduction - nsf.getBalance();
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
                        deduction = nsf.getBalance();
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

            try {
                MoneyTransactionRecord tx = _repo.accumulateAndStoreTransaction(
                    memberId, Currency.COINS, refundAmount, TransactionType.REFUND_GIVEN,
                    MessageBundle.tcompose("m.item_refund", itemName), item, false);
                updates.add(tx);
                coinPool -= refundAmount;
            } catch (DatabaseException de) {
                log.warning(de.getMessage()); // keep going with the refund
            }
        }

        for (MoneyTransactionRecord txRec : updates) {
            // TODO: logAction?
            _nodeActions.moneyUpdated(txRec, true); // notify members
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
        throws ServiceException
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

        // if that didn't throw an exception, we're good to go.
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
     *
     * @throws ServiceException if the user has already requested a bling cash out that has not yet
     * been fulfilled or the amount requested is below the minimum amount allowed for cashing out
     * or the user hsa cashed out too recently.
     */
    public BlingInfo requestCashOutBling (int memberId, int amount, CashOutBillingInfo info)
        throws ServiceException
    {
        MemberAccountRecord account = _repo.load(memberId);

        // If the user does not have the minimum amount required to cash out bling, don't allow
        // them to proceed
        if (amount < _runtime.money.minimumBlingCashOut) {
            throw new ServiceException(MoneyCodes.E_BELOW_MINIMUM_BLING);
        }

        // Ensure the account has the requested amount of bling and that it is currently not
        // requesting a cash out.
        int blingAmount = amount * 100;
        if (account.bling < blingAmount) {
            throw new InsufficientFundsException(Currency.BLING, account.bling);
        }
        if (_repo.getCurrentCashOutRequest(memberId) != null) {
            throw new ServiceException(MoneyCodes.E_ALREADY_CASHED_OUT);
        }

        long waitTime = getTimeToNextBlingCashOutRequest(memberId);
        if (waitTime > 0) {
            throw new ServiceException("e.cashed_out_too_recently");
        }

        // Add a cash out record for this member.
        BlingCashOutRecord cashOut = _repo.createCashOut(
            memberId, blingAmount, _runtime.money.blingWorth, info);

        return toBlingInfo(account, cashOut, CASHOUT_FREQUENCY);
    }

    /**
     * Calculates how long the given member must wait until requesting another cash out, in
     * milliseconds.
     */
    public long getTimeToNextBlingCashOutRequest (int memberId)
    {
        BlingCashOutRecord recent = _repo.getMostRecentBlingCashout(memberId);
        long lastTime = recent == null ? 0 : recent.timeRequested.getTime();
        return Math.max(0, lastTime - System.currentTimeMillis() + CASHOUT_FREQUENCY);
    }

    /**
     * Converts some amount of bling in a member's account into bars.
     *
     * @param memberId ID of the member.
     * @param blingAmount Amount of bling (NOT centibling) to convert to bars.
     *
     * @return the number of bars added to the account.
     *
     * @throws ServiceException if the account does not have the specified amount of bling
     * available.
     */
    public BlingExchangeResult exchangeBlingForBars (int memberId, int blingAmount)
        throws ServiceException
    {
        MoneyTransactionRecord deductTx = _repo.deductAndStoreTransaction(
            memberId, Currency.BLING, blingAmount * 100,
            TransactionType.SPENT_FOR_EXCHANGE, "m.exchanged_for_bars", null);
        // if that didn't throw an exception, we're good to go.
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
        return toBlingInfo(_repo.load(memberId), _repo.getCurrentCashOutRequest(memberId),
            getTimeToNextBlingCashOutRequest(memberId));
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
        Preconditions.checkArgument(memberId != 0, "Requested transactions for invalid member.");
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
     * Secures a price for an item. This ensures the user will be able to purchase an item for a
     * set price. This price will remain available for some amount of time (specified by {@link
     * PriceQuoteCache#SECURED_PRICE_DURATION}. The secured price may also be removed if the
     * maximum number of secured prices system-wide has been reached (specified by {@link
     * PriceQuoteCache#MAX_SECURED_PRICES}. In either case, an attempt to buy the item will fail
     * with a {@link CostUpdatedException}. If a guest id is specified, the quote is returned, but
     * not saved in the cache.
     *
     * @param buyerId the memberId of the buying user.
     * @param item the identity of the catalog listing.
     * @param listedCurrency the currency at which the item is listed.
     * @param listedAmount the amount at which the item is listed.
     *
     * @return a full PriceQuote for the item.
     */
    public PriceQuote securePrice (
        int buyerId, CatalogIdent item, Currency listedCurrency, int listedAmount)
    {
        Preconditions.checkArgument(isValid(item), "item is invalid: %s", item);
        // FixedExchange
        return securePrice(buyerId, (Object)item, listedCurrency, listedAmount,
            (listedCurrency == Currency.COINS));
/// FixedExchange
///        return securePrice(buyerId, (Object)item, listedCurrency, listedAmount);
    }

    /**
     * Secure a price for a generic ware, allowing exchange.
     */
    public PriceQuote securePrice (
        int buyerId, Object wareKey, Currency listedCurrency, int listedAmount)
    {
        return securePrice(buyerId, wareKey, listedCurrency, listedAmount, true);
    }

    /**
     */
    public PriceQuote securePrice (
        int buyerId, Object wareKey, Currency listedCurrency, int listedAmount,
        boolean allowExchange)
    {
        Preconditions.checkArgument(listedAmount >= 0, "listedAmount is invalid: %d", listedAmount);
        PriceQuote quote = _exchange.secureQuote(listedCurrency, listedAmount, allowExchange);
        _priceCache.addQuote(buyerId, wareKey, quote);
        return quote;
    }

    /**
     * Calculates the cost factor of a broadcast, which is multiplied by the configured
     * cost to arrive at the actual cost.
     */
    public float getRecentBroadcastFactor ()
    {
        long now = System.currentTimeMillis();
        // TODO: logarithmic decay
        int last10Mins = _repo.countBroadcastsSince(now - (10 * 60 * 1000));
        int over10MinsUnder2Hours = _repo.countBroadcastsSince(now - (2 * 60 * 60 * 1000))
            - last10Mins;
        return last10Mins + // within the last 10 minutes count fully
            (.25f * over10MinsUnder2Hours); // else only 1/4th as much.
    }

    /**
     * Is this CatalogIdent valid?
     */
    protected static boolean isValid (CatalogIdent ident)
    {
        return (ident != null) && (ident.type != Item.NOT_A_TYPE) && (ident.catalogId != 0);
    }

    /**
     * Compute the payouts that creators should receive for the given price quote. The first
     * listing is the creator who listed the item. Others are contributors. Payouts are made based
     * on a fraction of the total payout due, with the lister receiving any remainder.
     */
    protected List<CurrencyAmount> computeCreatorPayouts (
        PriceQuote quote, List<CatalogRecord> listings)
        throws ServiceException
    {
        // get the normal total creator payout - this is what we'll distribute
        CurrencyAmount totalPayout = computePayout(quote, _runtime.money.creatorPercentage);

        // optimize the single creator case with no bases
        if (listings.size() == 1) {
            return Collections.singletonList(totalPayout);
        }

        // all listings must match this currency - detecting race condition
        Currency baseCurrency = listings.get(listings.size() - 1).currency;

        // list of divided payouts
        List<CurrencyAmount> divvies = Lists.newArrayListWithExpectedSize(listings.size());

        // loop from the end and give each creator a portion of the payout
        int totalCost = listings.get(0).cost, lastCost = 0;
        for (int ii = listings.size() - 1; ii >= 0; --ii) {
            CatalogRecord listing = listings.get(ii);
            if (listing.currency != baseCurrency) {
                log.warning("Unexpected currency", "expected", baseCurrency,
                    "got", listing.currency, "itemType", listing.item.getType(),
                    "catalogId", listing.catalogId);
                throw new ServiceException(MoneyCodes.E_INTERNAL_ERROR);
            }

            // each creator gets a % of the payout based on the % of the cost he contributes
            float costContribution = (float)(listing.cost - lastCost) / totalCost;
            CurrencyAmount payout = new CurrencyAmount(totalPayout.currency, (int)(Math.floor(
                costContribution * totalPayout.amount)));
            divvies.add(payout);

            if (payout.amount == 0) {
                log.warning("Payout or cost too small for divvying", "totalCost", totalCost,
                    "listingPortion", listing.cost - lastCost, "totalPayout", totalPayout,
                    "itemType", listing.item.getType(), "catalogId", listing.catalogId);
                throw new ServiceException(MoneyCodes.E_INTERNAL_ERROR);
            }

            lastCost = listing.cost;
        }

        // reverse so the divvies match the incoming list
        Collections.reverse(divvies);

        // calculate the distributed payout so far
        int payoutSum = 0;
        for (CurrencyAmount amount : divvies) {
            payoutSum += amount.amount;
        }

        // since we use Math.floor, this should never happen
        if (payoutSum > totalPayout.amount) {
            log.warning("Fishy... basis payouts larger than overall payout", "payoutSum", payoutSum,
                "totalPayout", totalPayout, "itemType", listings.get(0).item.getType(),
                "catalogId", listings.get(0).catalogId);
            throw new ServiceException(MoneyCodes.E_INTERNAL_ERROR);
        }

        // since we use Math.floor, complete payout by giving leftovers to the main creator
        if (payoutSum < totalPayout.amount) {
            divvies.get(0).amount += totalPayout.amount - payoutSum;
        }

        return divvies;
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
     * Creates a new bling info using the provided parameters and our current runtime settings.
     */
    protected BlingInfo toBlingInfo (
        MemberAccountRecord account, BlingCashOutRecord pendingCashOut, long waitTime)
    {
        return new BlingInfo(account.bling, _runtime.money.blingWorth,
            _runtime.money.minimumBlingCashOut * 100, waitTime,
            pendingCashOut == null ? null : pendingCashOut.toInfo());
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
    protected static <K, V> V getOrCreate (Map<K, V> map, K key, Function<K, V> createFunc)
    {
        V value = map.get(key);
        if (value == null) {
            map.put(key, value = createFunc.apply(key));
        }
        return value;
    }

    protected static class IntermediateBuyResult<T>
    {
        /** Was this a magic-free transaction? */
        public boolean magicFree;

        /** The price quote that was used for the purchase. */
        public PriceQuote quote;

        /** A fully-stored MoneyTransaction representing the money removed from the buyer. */
        public MoneyTransactionRecord buyerTx;

        /** A fully-stored MoneyTransaction representing the change to the buyer, or null. */
        public MoneyTransactionRecord changeTx;

        /** The result of the buy operation. */
        public T ware;

        /** Mr. Constructor */
        public IntermediateBuyResult (
            boolean magicFree, PriceQuote quote,
            MoneyTransactionRecord buyerTx, MoneyTransactionRecord changeTx, T ware)
        {
            this.magicFree = magicFree;
            this.quote = quote;
            this.buyerTx = buyerTx;
            this.changeTx = changeTx;
            this.ware = ware;
        }
    }

    /** A Function that transforms a CashOutRecord into a CashOutInfo. */
    protected final Function<BlingCashOutRecord, CashOutInfo> TO_CASH_OUT_INFO =
        new Function<BlingCashOutRecord, CashOutInfo>() {
            public CashOutInfo apply (BlingCashOutRecord record) {
                return record.toInfo();
            }
        };

    protected PriceQuoteCache _priceCache = new PriceQuoteCache();

    // dependencies
    @Inject protected BlingPoolDistributor _blingDistributor;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected MoneyExchange _exchange;
    @Inject protected MoneyMessageListener _msgReceiver;
    @Inject protected MoneyNodeActions _nodeActions;
    @Inject protected MoneyRepository _repo;
    @Inject protected MoneyTransactionExpirer _expirer;
    @Inject protected MsoyEventLogger _eventLog;
    @Inject protected RuntimeConfig _runtime;
    @Inject protected UserActionRepository _userActionRepo;

    /** An arbitrary key for tracking quotes in {@link #listItem}. */
    protected static final Object LIST_ITEM_KEY = new Object();

    /** We don't service bling cashouts at any lower frequency than this. */
    protected static final long CASHOUT_FREQUENCY = BlingInfo.CASHOUT_DAYS * 24 * 60 * 60 * 1000L;
}
