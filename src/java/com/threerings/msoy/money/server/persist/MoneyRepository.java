//
// $Id$

package com.threerings.msoy.money.server.persist;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.jcip.annotations.NotThreadSafe;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.CacheInvalidator.TraverseWithFilter;
import com.samskivert.depot.DataMigration;
import com.samskivert.depot.DatabaseException;
import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.DuplicateKeyException;
import com.samskivert.depot.Key;
import com.samskivert.depot.Ops;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.SchemaMigration;
import com.samskivert.depot.clause.FromOverride;
import com.samskivert.depot.clause.Limit;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.clause.QueryClause;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.util.Logger;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.server.persist.CountRecord;

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

        _ctx.registerMigration(MoneyTransactionRecord.class, new SchemaMigration(6) {
            protected int invoke (Connection conn, DatabaseLiaison liaison) throws SQLException {
                String ixName = _tableName + "_ixCurrency";
                if (liaison.tableContainsIndex(conn, _tableName, ixName)) {
                    log.info("Dropping index " + ixName + " using liaison: " + liaison);
                    liaison.dropIndex(conn, _tableName, ixName);
                    return 1;
                }
                log.warning(_tableName + " does not contain index " + ixName + " for dropping.");
                return 0;
            }
        });

        if (!DeploymentConfig.devDeployment) {
            registerMigration(new DataMigration("2009_02_12_dumpBarsIntoExchange") {
                @Override public void invoke () throws DatabaseException {
                    adjustBarPool(15000);
                }
            });
        }
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

        ColumnExp currencyCol = MemberAccountRecord.getColumn(currency);
        Map<ColumnExp,SQLExpression> updates = Maps.newHashMap();
        updates.put(currencyCol, currencyCol.plus(amount));
        if (updateAcc) {
            ColumnExp currencyAccCol = MemberAccountRecord.getAccColumn(currency);
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
        return new MoneyTransactionRecord(memberId, currency, amount, acct.getAmount(currency),
            updateAcc, acct.getAccAmount(currency));
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

        ColumnExp currencyCol = MemberAccountRecord.getColumn(currency);
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
        return new MoneyTransactionRecord(memberId, currency, (count == 0) ? 0 : -amount, balance,
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
        ColumnExp currencyCol = MemberAccountRecord.getColumn(deduction.currency);
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

    public List<MoneyTransactionRecord> getTransactions (
        int memberId, Set<TransactionType> transactionTypes, Currency currency,
        int start, int count, boolean descending)
    {
        // select * from MemberAccountRecord where type = ? and transactionType in (?)
        // and memberId=? order by timestamp
        List<QueryClause> clauses = Lists.newArrayList();
        populateSearch(clauses, memberId, transactionTypes, currency);

        clauses.add(descending ?
                    OrderBy.descending(MoneyTransactionRecord.TIMESTAMP) :
                    OrderBy.ascending(MoneyTransactionRecord.TIMESTAMP));

        if (count != Integer.MAX_VALUE) {
            clauses.add(new Limit(start, count));
        }

        return findAll(MoneyTransactionRecord.class, clauses);
    }

    public int getTransactionCount (
        int memberId, Set<TransactionType> transactionTypes, Currency currency)
    {
        List<QueryClause> clauses = Lists.newArrayList();
        clauses.add(new FromOverride(MoneyTransactionRecord.class));
        populateSearch(clauses, memberId, transactionTypes, currency);
        return load(CountRecord.class, clauses.toArray(new QueryClause[clauses.size()])).count;
    }

    public int deleteOldTransactions (Currency currency, long maxAge)
    {
        Timestamp cutoff = new Timestamp(System.currentTimeMillis() - maxAge);
        Where where = new Where(
            Ops.and(MoneyTransactionRecord.CURRENCY.eq(currency),
                    MoneyTransactionRecord.TIMESTAMP.lessThan(cutoff)));
        return deleteAll(MoneyTransactionRecord.class, where, null /* no cache invalidation */);
    }

    /**
     * Loads recent transactions that were inserted with the given subject.
     * @param subject the subject of the transaction to search for
     * @param from offset in the list of transactions to return first
     * @param count maximum number of transactions to return
     * @param descending if set, transactions are ordered newest to oldest
     */
    public List<MoneyTransactionRecord> getTransactionsForSubject (
        Object subject, int from, int count, boolean descending)
    {
        List<QueryClause> clauses = Lists.newArrayList();
        clauses.add(makeSubjectSearch(subject));

        clauses.add(descending ?
            OrderBy.descending(MoneyTransactionRecord.TIMESTAMP) :
            OrderBy.ascending(MoneyTransactionRecord.TIMESTAMP));

        if (count != Integer.MAX_VALUE) {
            clauses.add(new Limit(from, count));
        }

        return findAll(MoneyTransactionRecord.class, clauses);
    }

    /**
     * Loads the number of recent transactions that were inserted with the given subject.
     * @param subject the subject of the transaction to search for
     */
    public int getTransactionCountForSubject (Object subject)
    {
        List<QueryClause> clauses = Lists.newArrayList();
        clauses.add(new FromOverride(MoneyTransactionRecord.class));
        clauses.add(makeSubjectSearch(subject));
        return load(CountRecord.class, clauses.toArray(new QueryClause[clauses.size()])).count;
    }

    /**
     * Get the number of bars and the coin balance in the bar pool.
     */
    public int[] getBarPool (int defaultBarPoolSize)
    {
        BarPoolRecord bpRec = load(BarPoolRecord.class, BarPoolRecord.KEY);
        if (bpRec == null) {
            bpRec = createBarPoolRecord(defaultBarPoolSize);
        }
        return new int[] { bpRec.barPool, bpRec.coinBalance };
    }

    /**
     * Adjust the bar pool as a result of an exchange.
     *
     * @param barDelta a positive number if bars were used to purchase coin-listed stuff
     *                 a negative number if coins were used to purchase bar-listed stuff.
     */
    public void recordExchange (int barDelta, int coinDelta, float rate, int referenceTxId)
    {
        updatePartial(BarPoolRecord.class, BarPoolRecord.KEY, BarPoolRecord.KEY,
                      BarPoolRecord.BAR_POOL, BarPoolRecord.BAR_POOL.plus(barDelta),
                      BarPoolRecord.COIN_BALANCE, BarPoolRecord.COIN_BALANCE.plus(coinDelta));
        insert(new ExchangeRecord(barDelta, coinDelta, rate, referenceTxId));
    }

    public List<ExchangeRecord> getExchangeData (int start, int count)
    {
        return findAll(ExchangeRecord.class, OrderBy.descending(ExchangeRecord.TIMESTAMP),
            new Limit(start, count));
    }

    /**
     * Adjust the bar pool.
     * This is used in two circumstances:
     * 1. When we react to adjustments in the *size* of the target bar pool size, we automatically
     * remove or add bars.
     * 2. Sometimes Daniel dumps more bars in, cuz we're crazy like that. These are done manually.
     */
    public void adjustBarPool (int delta)
    {
        updatePartial(BarPoolRecord.class, BarPoolRecord.KEY, BarPoolRecord.KEY,
                      BarPoolRecord.BAR_POOL, BarPoolRecord.BAR_POOL.plus(delta));
    }

    public int getExchangeDataCount ()
    {
        return load(CountRecord.class, new FromOverride(ExchangeRecord.class)).count;
    }

    public int deleteOldExchangeRecords (long maxAge)
    {
        final long oldestTimestamp = System.currentTimeMillis() - maxAge;
        return deleteAll(ExchangeRecord.class, new Where(ExchangeRecord.TIMESTAMP.lessThan(
                                                             new Timestamp(oldestTimestamp))));
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
        // we delete transaction records for both (TODO: maybe we should preserve tx records for
        // deleted members for a while... oh the complexities)
        deleteAll(MoneyTransactionRecord.class,
                  new Where(MoneyTransactionRecord.MEMBER_ID.in(
                                Sets.union(Sets.newHashSet(guestIds),
                                           Sets.newHashSet(memberIds)))));
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

    /**
     * Create the singleton BarPoolRecord in the database.
     */
    protected BarPoolRecord createBarPoolRecord (int defaultBarPoolSize)
    {
        BarPoolRecord bpRec = new BarPoolRecord();
        bpRec.id = BarPoolRecord.RECORD_ID;
        bpRec.barPool = defaultBarPoolSize;
        try {
            insert(bpRec);
            // log a warning, hopefully we ever only do this once.
            log.warning("Populated initial exchange bar pool");
        } catch (Exception e) {
            // hmm, beaten to the punch?
            bpRec = load(BarPoolRecord.class, BarPoolRecord.KEY);
            if (bpRec == null) {
                throw new DatabaseException("What in the whirled? Can't populate BarPoolRecord.");
            }
        }
        return bpRec;
    }

    /** Helper method to setup a query for a transaction history search. */
    protected void populateSearch (
        List<QueryClause> clauses, int memberId,
        Set<TransactionType> transactionTypes, Currency currency)
    {
        List<SQLExpression> where = Lists.newArrayList();

        where.add(MoneyTransactionRecord.MEMBER_ID.eq(memberId));
        if (transactionTypes != null) {
            where.add(MoneyTransactionRecord.TRANSACTION_TYPE.in(transactionTypes));
        }
        if (currency != null) {
            where.add(MoneyTransactionRecord.CURRENCY.eq(currency));
        }

        clauses.add(new Where(Ops.and(where)));
    }

    protected Where makeSubjectSearch (Object subject)
    {
        MoneyTransactionRecord.Subject subj = new MoneyTransactionRecord.Subject(subject);
        List<SQLExpression> where = Lists.newArrayList();
        where.add(MoneyTransactionRecord.SUBJECT_TYPE.eq(subj.type));
        where.add(MoneyTransactionRecord.SUBJECT_ID_TYPE.eq(subj.idType));
        where.add(MoneyTransactionRecord.SUBJECT_ID.eq(subj.id));
        return new Where(Ops.and(where));
    }

    @Override
    protected void getManagedRecords (final Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(MemberAccountRecord.class);
        classes.add(MoneyTransactionRecord.class);
        classes.add(MoneyConfigRecord.class);
        classes.add(BlingCashOutRecord.class);
        classes.add(BarPoolRecord.class);
        classes.add(ExchangeRecord.class);
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
