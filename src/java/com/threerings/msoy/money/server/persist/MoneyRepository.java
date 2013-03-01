//
// $Id$

package com.threerings.msoy.money.server.persist;

import java.io.Serializable;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import net.jcip.annotations.NotThreadSafe;

import com.samskivert.util.Logger;

import com.samskivert.depot.CacheInvalidator.TraverseWithFilter;
import com.samskivert.depot.CountRecord;
import com.samskivert.depot.DataMigration;
import com.samskivert.depot.DatabaseException;
import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.DuplicateKeyException;
import com.samskivert.depot.Exps;
import com.samskivert.depot.Key;
import com.samskivert.depot.Ops;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.clause.FromOverride;
import com.samskivert.depot.clause.Limit;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.clause.QueryClause;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.expression.SQLExpression;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.money.data.all.CashOutBillingInfo;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.TransactionType;
import com.threerings.msoy.money.gwt.InsufficientFundsException;

import static com.threerings.msoy.Log.log;

/**
 * Interface for retrieving and persisting entities in the money service.
 *
 * @author Kyle Sampson <kyle@threerings.net>
 * @author Ray Greenwell <ray@threerings.net>
 */
@Singleton
@BlockingThread
@NotThreadSafe
public class MoneyRepository extends DepotRepository
{
    @Inject
    public MoneyRepository (final PersistenceContext ctx)
    {
        super(ctx);

        registerMigration(new DataMigration("2013-03 Account bars and bling removal") {
            @Override public void invoke ()
                throws DatabaseException {
                int coinsPerBar = 10000;
                int coinsPerCentiBling = coinsPerBar/100;
                Where all = new Where(Exps.value(true).eq(true));

                updatePartial(MemberAccountRecord._R, all, null,
                    MemberAccountRecord.COINS, MemberAccountRecord.COINS.plus(
                        MemberAccountRecord.BARS.times(coinsPerBar).plus(
                        MemberAccountRecord.BLING.times(coinsPerCentiBling))),

                    MemberAccountRecord.ACC_COINS, MemberAccountRecord.ACC_COINS.plus(
                        MemberAccountRecord.BARS.times(coinsPerBar).plus(
                        MemberAccountRecord.BLING.times(coinsPerCentiBling)))
                );
            }
        });
    }

    /**
     * Create a MemberAccountRecord when we create an account.
     */
    public MemberAccountRecord create (int memberId)
    {
        MemberAccountRecord rec = new MemberAccountRecord(memberId);
        insert(rec);
        return rec;
    }

    /**
     * Retrieves a member's account info by member ID.
     */
    public MemberAccountRecord load (int memberId)
    {
        return load(MemberAccountRecord.getKey(memberId));
    }

    /**
     * Retries all member accounts from the given list of member IDs.
     */
    public List<MemberAccountRecord> loadAll (Set<Integer> memberIds)
    {
        return loadAll(MemberAccountRecord.class, memberIds);
    }

    /**
     * Accumulate money, return a partially-populated MoneyTransactionRecord for storing.
     *
     * @param updateAcc true if this transaction is "accumulating", meaning that they earned the
     * coins rather than got them through change, for example.
     */
    protected MoneyTransactionRecord accumulate (
        int memberId, Currency currency, int amount, boolean updateAcc)
    {
        Preconditions.checkArgument(amount >= 0, "Amount to accumulate must be 0 or greater.");

        ColumnExp<Integer> currencyCol = MemberAccountRecord.getColumn(currency);
        Map<ColumnExp<?>,SQLExpression<?>> updates = Maps.newHashMap();
        updates.put(currencyCol, currencyCol.plus(amount));
        if (updateAcc) {
            ColumnExp<Long> currencyAccCol = MemberAccountRecord.getAccColumn(currency);
            updates.put(currencyAccCol, currencyAccCol.plus(amount));
        }

        Key<MemberAccountRecord> key = MemberAccountRecord.getKey(memberId);
        int count = updatePartial(key, updates);
        if (count == 0) {
            // accumulate should always work, so if we mod'd 0 rows, it means there's no member
            throw new DatabaseException(
                Logger.format("Missing member in accumulate?!", "id", memberId,
                              "currency", currency, "amount", amount, "ua", updateAcc));
        }

        // TODO: be able to get the balance at the same time as the update, pending Depot changes
        MemberAccountRecord acct = load(MemberAccountRecord.class, key);
        return createTransactionRecord(currency, memberId, amount, acct.getAmount(currency),
            updateAcc, acct.getAccAmount(currency));

    }

    protected <T> ColumnExp<T> getColumn (Currency currency, ColumnExp<T> pcol)
    {
        return pcol.as(getTransactionRecordClass(currency));
    }

    protected Class<? extends MoneyTransactionRecord> getTransactionRecordClass (Currency currency)
    {
        switch(currency) {
        case COINS:
            return CoinMoneyTransactionRecord.class;
        case BARS:
            return BarMoneyTransactionRecord.class;
        case BLING:
            return BlingMoneyTransactionRecord.class;
        default:
            throw new IllegalArgumentException("Unknown currency: " + currency);
        }
    }

    protected MoneyTransactionRecord createTransactionRecord (Currency currency, int memberId,
        int amount, int balance, boolean updateAcc, long accBalance)
    {
        switch(currency) {
        case COINS:
            return new CoinMoneyTransactionRecord(memberId, amount, balance,
                updateAcc, accBalance);
        case BARS:
            return new BarMoneyTransactionRecord(memberId, amount, balance,
                updateAcc, accBalance);
        case BLING:
            return new BlingMoneyTransactionRecord(memberId, amount, balance,
                updateAcc, accBalance);
        default:
            throw new IllegalArgumentException("Unknown currency: " + currency);
        }
    }

    /**
     * Deduct money from the specified member's money.  If the user does not have enough money, an
     * InsufficientFundsException will be thrown.
     *
     * @return a partially filled-out MTR that should later either be passed to rollbackDeduction()
     * or filled-in and passed to storeTransaction().
     */
    public MoneyTransactionRecord deduct (
        int memberId, Currency currency, int amount, boolean allowFree)
        throws InsufficientFundsException
    {
        Preconditions.checkArgument(amount >= 0, "Amount to deduct must be 0 or greater.");

        ColumnExp<Integer> currencyCol = MemberAccountRecord.getColumn(currency);
        Key<MemberAccountRecord> key = MemberAccountRecord.getKey(memberId);
        Where where = new Where(Ops.and(MemberAccountRecord.MEMBER_ID.eq(memberId),
                                        currencyCol.greaterEq(amount)));

        int count = updatePartial(MemberAccountRecord.class, where, key,
                                  currencyCol, currencyCol.minus(amount));
        // TODO: be able to get the balance at the same time as the update, pending Depot changes
        MemberAccountRecord mar = load(MemberAccountRecord.class, key);
        if (mar == null) {
            throw new DatabaseException(
                Logger.format("Missing member in deduct?!", "id", memberId, "currency", currency,
                              "amount", amount, "allowFree", allowFree));
        }

        int balance = mar.getAmount(currency);
        if (count == 0 && !allowFree) {
            throw new InsufficientFundsException(currency, balance);
        }

        // Return the amount reserved, or 0 if it didn't work, but allowFree==true
        return createTransactionRecord(currency, memberId, (count == 0) ? 0 : -amount, balance,
            false, mar.getAccAmount(currency));
    }

    /**
     * Deduct money from the specified member's money and immediately store the
     * transaction.
     */
    public MoneyTransactionRecord deductAndStoreTransaction (
        int memberId, Currency currency, int amount, TransactionType type,
        String description, Object subject)
        throws InsufficientFundsException
    {
        MoneyTransactionRecord tx = deduct(memberId, currency, amount, false);
        tx.fill(type, description, subject);
        storeTransaction(tx);
        return tx;
    }

    /**
     * Accumulate and store the specified MoneyTransaction.
     */
    public MoneyTransactionRecord accumulateAndStoreTransaction (
        int memberId, Currency currency, int amount,
        TransactionType type, String description, Object subject, boolean updateAcc)
    {
        MoneyTransactionRecord tx = accumulate(memberId, currency, amount, updateAcc);
        tx.fill(type, description, subject);
        storeTransaction(tx);
        return tx;
    }

    /**
     * Accumulate and store the specified MoneyTransaction.
     */
    public MoneyTransactionRecord accumulateAndStoreTransaction (
        int memberId, Currency currency, int amount,
        TransactionType type, String description, Object subject,
        int referenceTxId, int referenceMemberId, boolean updateAcc)
    {
        MoneyTransactionRecord tx = accumulate(memberId, currency, amount, updateAcc);
        tx.fill(type, description, subject);
        tx.referenceTxId = referenceTxId;
        tx.referenceMemberId = referenceMemberId;
        storeTransaction(tx);
        return tx;
    }

    /**
     * Rollback a deduction. The transaction is not saved, it is merely used to reference
     * the values.
     */
    public void rollbackDeduction (MoneyTransactionRecord deduction)
    {
        Preconditions.checkArgument(deduction.amount <= 0, "Only deductions can be rolled back.");
        Preconditions.checkArgument(deduction.id == 0, "Transaction has already been inserted!");
        ColumnExp<Integer> currencyCol = MemberAccountRecord.getColumn(deduction.getCurrency());
        Key<MemberAccountRecord> key = MemberAccountRecord.getKey(deduction.memberId);
        if (updatePartial(MemberAccountRecord.class, key, key,
                          currencyCol, currencyCol.plus(-deduction.amount)) == 0) {
            log.warning("Missing member for rollback?!", "mtr", deduction);
        }
    }

    /**
     * Store a fully-populated transaction that has come from deduct or accumulate.
     */
    public void storeTransaction (MoneyTransactionRecord transaction)
    {
        Preconditions.checkArgument(transaction.id == 0,
            "Transaction has already been inserted!");
        Preconditions.checkArgument(transaction.transactionType != null,
            "TransactionType must be populated.");
        Preconditions.checkArgument(transaction.description != null,
            "Description must be populated.");

        insert(transaction);
    }

    public List<? extends MoneyTransactionRecord> getTransactions (
        int memberId, Set<TransactionType> transactionTypes, Currency currency,
        int start, int count, boolean descending)
    {
        List<SQLExpression<?>> where = Lists.newArrayList();
        where.add(getColumn(currency, MoneyTransactionRecord.MEMBER_ID).eq(memberId));
        if (transactionTypes != null) {
            where.add(getColumn(currency, MoneyTransactionRecord.TRANSACTION_TYPE)
                .in(transactionTypes));
        }

        return findAll(getTransactionRecordClass(currency),
            new Where(Ops.and(where)),
            descending ?
                OrderBy.descending(getColumn(currency, MoneyTransactionRecord.TIMESTAMP)) :
                OrderBy.ascending(getColumn(currency, MoneyTransactionRecord.TIMESTAMP)),
            new Limit(start, count));
    }

    public List<BlingMoneyTransactionRecord> getBlingPoolTransactions (long start, long end)
    {
        return findAll(BlingMoneyTransactionRecord._R, new Where(Ops.and(
            BlingMoneyTransactionRecord.TRANSACTION_TYPE.eq(TransactionType.BLING_POOL),
            BlingMoneyTransactionRecord.TIMESTAMP.greaterThan(new Timestamp(start)),
            BlingMoneyTransactionRecord.TIMESTAMP.lessThan(new Timestamp(end)))));
    }

    public int deleteOldTransactions (Currency currency, long maxAge)
    {
        Timestamp cutoff = new Timestamp(System.currentTimeMillis() - maxAge);
        return deleteAll(getTransactionRecordClass(currency),
            new Where(getColumn(currency, MoneyTransactionRecord.TIMESTAMP).lessThan(cutoff)));
    }

    /**
     * Loads recent transactions that were inserted with the given subject.
     * @param subject the subject of the transaction to search for
     * @param from offset in the list of transactions to return first
     * @param count maximum number of transactions to return
     * @param descending if set, transactions are ordered newest to oldest
     */
    public List<? extends MoneyTransactionRecord> getTransactionsForSubject (
        Object subject, Currency currency, int start, int count, boolean descending)
    {
        List<QueryClause> clauses = Lists.newArrayList();
        if (count != Integer.MAX_VALUE) {
            clauses.add(new Limit(start, count));
        }
        clauses.add(makeSubjectSearch(currency, subject));
        clauses.add(descending ?
            OrderBy.descending(getColumn(currency, MoneyTransactionRecord.TIMESTAMP)) :
            OrderBy.ascending(getColumn(currency, MoneyTransactionRecord.TIMESTAMP)));

        return findAll(getTransactionRecordClass(currency), clauses);
    }

    /**
     * Loads the current money configuration record, optionally locking on the record.
     *
     * @param lock If true, the record will be selected using SELECT ... FOR UPDATE to grab
     * a lock on the record.  If another process has already locked this record, this will return
     * null.
     * @return The money configuration, or null if we tried locking the record, but it was already
     * locked.
     */
    public MoneyConfigRecord getMoneyConfig (final boolean lock)
    {
        if (lock) {
            // Update the record, setting locked = true, but only if locked = false currently.
            int count = updatePartial(MoneyConfigRecord.class,
                new Where(MoneyConfigRecord.LOCKED, false),
                MoneyConfigRecord.getKey(MoneyConfigRecord.RECORD_ID),
                MoneyConfigRecord.LOCKED, true);
            if (count == 0) {
                // Record is already locked, bail out.
                return null;
            }
        }

        MoneyConfigRecord confRecord = load(MoneyConfigRecord.class);
        if (confRecord == null) {
            // Create a new money config record with the current date for the last time bling
            // was distributed and save it.  If something else slips in and adds it, this will
            // throw an exception -- just attempt to retrieve it at that point.
            confRecord = new MoneyConfigRecord();
            try {
                store(confRecord);
            } catch (DuplicateKeyException dke) {
                confRecord = load(MoneyConfigRecord.class);
            }
        }
        return confRecord;
    }

    /**
     * Sets the last time bling was distributed to the given date.  This will also unlock the
     * record; this should only be called if we have the lock on the record and wish to release it.
     *
     * @param lastDistributedBling Date the bling was last distributed.
     */
    public void completeBlingDistribution (Date lastDistributedBling)
    {
        updatePartial(MoneyConfigRecord.getKey(MoneyConfigRecord.RECORD_ID),
            MoneyConfigRecord.LOCKED, false,
            MoneyConfigRecord.LAST_DISTRIBUTED_BLING, lastDistributedBling);
    }

    /**
     * Commits a bling cash out request.  This will only update the CashOutRecord -- bling deduction
     * must be handled separately.
     *
     * @param memberId ID of the member to commit.
     * @param actualAmount Actual amount of centibling that was cashed out.
     * @return Number of records updated, either 0 or 1.  Can be zero if the member is not currently
     * cashing out any bling.
     */
    public int commitBlingCashOutRequest (int memberId, int actualAmount)
    {
        Where where = new Where(Ops.and(
            BlingCashOutRecord.MEMBER_ID.eq(memberId),
            BlingCashOutRecord.TIME_FINISHED.isNull()));
        return updatePartial(BlingCashOutRecord.class, where,
            new ActiveCashOutInvalidator(memberId),
            BlingCashOutRecord.TIME_FINISHED, new Timestamp(System.currentTimeMillis()),
            BlingCashOutRecord.ACTUAL_CASHED_OUT, actualAmount,
            BlingCashOutRecord.SUCCESSFUL, true);
    }

    /**
     * Cancels a request to cash out bling.
     *
     * @param memberId ID of the member whose bling has been cashed out.
     * @param reason The reason the cash out failed.
     * @return The number of records, either 0 or 1.  If 0, there are no active cash outs for the
     * user.
     */
    public int cancelBlingCashOutRequest (int memberId, String reason)
    {
        Where where = new Where(Ops.and(
            BlingCashOutRecord.MEMBER_ID.eq(memberId),
            BlingCashOutRecord.TIME_FINISHED.isNull()));
        return updatePartial(BlingCashOutRecord.class, where,
            new ActiveCashOutInvalidator(memberId),
            BlingCashOutRecord.TIME_FINISHED, new Timestamp(System.currentTimeMillis()),
            BlingCashOutRecord.CANCEL_REASON, reason,
            BlingCashOutRecord.SUCCESSFUL, false);
    }

    /**
     * Creates a new CashOutRecord that indicates the specified member requested a cash out.
     *
     * @param memberId id of the member cashing out.
     * @param blingAmount amount of centibling, to cash out.
     * @param blingWorth worth of each bling at the time the request was made.
     * @param info billing information indicating how the member should be paid.
     *
     * @return the newly created cash out record.
     */
    public BlingCashOutRecord createCashOut (int memberId, int blingAmount, int blingWorth,
        CashOutBillingInfo info)
    {
        BlingCashOutRecord cashOut = new BlingCashOutRecord(
            memberId, blingAmount, blingWorth, info);
        insert(cashOut);
        return cashOut;
    }

    /**
     * Retrieves the current cash out request for the specified user.
     *
     * @param memberId ID of the member to retrieve a cash out record for.
     * @return The current cash out record, or null if the user is not currently cashing out.
     */
    public BlingCashOutRecord getCurrentCashOutRequest (int memberId)
    {
        return load(BlingCashOutRecord.class, new Where(Ops.and(
            BlingCashOutRecord.TIME_FINISHED.isNull(),
            BlingCashOutRecord.MEMBER_ID.eq(memberId))));
    }

    /**
     * Retrieves all cash out records for members that are currently cashing out some amount
     * of their bling.
     */
    public List<BlingCashOutRecord> getAccountsCashingOut ()
    {
        // select * from CashOutRecord where timeCompleted is null
        return findAll(BlingCashOutRecord.class, new Where(
            BlingCashOutRecord.TIME_FINISHED.isNull()));
    }

    /**
     * Retrieves the most recent bling cash out request for a given member, either pending or
     * successful. Returns null if there are no cash outs.
     */
    public BlingCashOutRecord getMostRecentBlingCashout (int memberId)
    {
        List<BlingCashOutRecord> cashouts = findAll(BlingCashOutRecord.class,
            new Where(Ops.and(
                BlingCashOutRecord.MEMBER_ID.eq(memberId),
                Ops.or(BlingCashOutRecord.SUCCESSFUL,
                       BlingCashOutRecord.TIME_FINISHED.isNull()))),
            OrderBy.descending(BlingCashOutRecord.TIME_REQUESTED), new Limit(0, 1));
        return cashouts.size() > 0 ? cashouts.get(0) : null;
    }

    /**
     * Deletes all data associated with the supplied members. This is done as a part of purging
     * member accounts.
     */
    public void purgeMembers (Collection<Integer> guestIds, Collection<Integer> memberIds)
    {
        // we only delete money records for permaguests
        deleteAll(MemberAccountRecord.class,
                  new Where(MemberAccountRecord.MEMBER_ID.in(guestIds)));

        // delete COINS transactions (not very interesting), but keep a trail of bars/bling
        deleteAll(getTransactionRecordClass(Currency.COINS),
            new Where(getColumn(Currency.COINS, MoneyTransactionRecord.MEMBER_ID).in(
                Sets.union(Sets.newHashSet(guestIds), Sets.newHashSet(memberIds)))));
    }

    /**
     * Counts the number of broadcasts sent since a given time.
     */
    public int countBroadcastsSince (long time)
    {
        Timestamp limit = new Timestamp(time);
        return load(CountRecord.class, CacheStrategy.NONE,
                    new Where(BroadcastHistoryRecord.TIME_SENT.greaterThan(limit)),
                    new FromOverride(BroadcastHistoryRecord.class)).count;
    }

    /**
     * Inserts a new broadcast history record with the given parameters.
     */
    public void noteBroadcastPurchase (int memberId, int barsPaid, String message)
    {
        BroadcastHistoryRecord record = new BroadcastHistoryRecord();
        record.timeSent = new Timestamp(System.currentTimeMillis());
        record.barsPaid = barsPaid;
        record.memberId = memberId;
        record.message = message;
        insert(record);
    }

    /**
     * Counts the total number of paid broadcasts made.
     */
    public int countBroadcastHistoryRecords()
    {
        return load(CountRecord.class, new FromOverride(BroadcastHistoryRecord.class)).count;
    }

    /**
     * Gets a page of historical paid broadcasts.
     */
    public List<BroadcastHistoryRecord> getBroadcastHistoryRecords(int offset, int count)
    {
        return findAll(BroadcastHistoryRecord.class, OrderBy.descending(
            BroadcastHistoryRecord.TIME_SENT), new Limit(offset, count));
    }

    protected Where makeSubjectSearch (Currency currency, Object subject)
    {
        MoneyTransactionRecord.Subject subj = new MoneyTransactionRecord.Subject(subject);

        List<SQLExpression<?>> where = Lists.newArrayList();
        where.add(getColumn(currency, MoneyTransactionRecord.SUBJECT_TYPE).eq(subj.type));
        where.add(getColumn(currency, MoneyTransactionRecord.SUBJECT_ID_TYPE).eq(subj.idType));
        where.add(getColumn(currency, MoneyTransactionRecord.SUBJECT_ID).eq(subj.id));
        return new Where(Ops.and(where));
    }

    @Override
    protected void getManagedRecords (final Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(MemberAccountRecord.class);
        classes.add(CoinMoneyTransactionRecord.class);
        classes.add(BarMoneyTransactionRecord.class);
        classes.add(BlingMoneyTransactionRecord.class);
        classes.add(MoneyConfigRecord.class);
        classes.add(BlingCashOutRecord.class);
        classes.add(BroadcastHistoryRecord.class);
    }

    /** Cache invalidator that invalidates a member's current cash out record. */
    protected static class ActiveCashOutInvalidator extends TraverseWithFilter<BlingCashOutRecord>
    {
        public ActiveCashOutInvalidator (int memberId)
        {
            super(BlingCashOutRecord.class);
            _memberId = memberId;
        }

        protected boolean testForEviction (Serializable key, BlingCashOutRecord record)
        {
            return record.memberId == _memberId && record.timeFinished == null;
        }

        protected final int _memberId;
    }
}
