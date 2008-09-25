//
// $Id$

package com.threerings.msoy.money.server.persist;

import java.sql.Date;
import java.sql.Timestamp;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.jcip.annotations.NotThreadSafe;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.jdbc.depot.DatabaseException;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.DuplicateKeyException;
import com.samskivert.jdbc.depot.EntityMigration;
import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.clause.FromOverride;
import com.samskivert.jdbc.depot.clause.Limit;
import com.samskivert.jdbc.depot.clause.OrderBy;
import com.samskivert.jdbc.depot.clause.QueryClause;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.jdbc.depot.expression.ColumnExp;
import com.samskivert.jdbc.depot.expression.SQLExpression;
import com.samskivert.jdbc.depot.operator.Arithmetic;
import com.samskivert.jdbc.depot.operator.SQLOperator;
import com.samskivert.jdbc.depot.operator.Conditionals;
import com.samskivert.jdbc.depot.operator.Logic.And;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.server.persist.CountRecord;

import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.TransactionType;

import com.threerings.msoy.money.server.NotEnoughMoneyException;

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
    /**
     * Thrown when accumulate*() or deduct*() are called with an invalid memberId.
     */
    public static class NoSuchMemberException extends DatabaseException
    {
        public NoSuchMemberException (int memberId)
        {
            super("Member does not have a money record (" + memberId + ")");
        }
    }

    @Inject
    public MoneyRepository (final PersistenceContext ctx,
        /* TODO REMOVE */ final com.threerings.msoy.server.persist.MemberRepository mrepo)
    {
        super(ctx);

        ctx.registerMigration(MemberAccountRecord.class,
            new EntityMigration.Retype(4, MemberAccountRecord.BLING));
        ctx.registerMigration(MemberAccountRecord.class,
            new EntityMigration.Retype(4, MemberAccountRecord.ACC_BLING));
        ctx.registerMigration(MoneyConfigRecord.class, 
            new EntityMigration.Retype(3, MoneyConfigRecord.LAST_DISTRIBUTED_BLING));
        ctx.registerMigration(MemberAccountRecord.class, new EntityMigration.Drop(5, "versionId"));
        ctx.registerMigration(MemberAccountRecord.class,
            new EntityMigration.Drop(5, "dateLastUpdated"));
        ctx.registerMigration(MemberAccountRecord.class, new EntityMigration(6) {
            @Override public int invoke (
                java.sql.Connection conn, com.samskivert.jdbc.DatabaseLiaison liaison)
                throws java.sql.SQLException
            {
                try {
                return mrepo.runMemberMigration(
                new com.threerings.msoy.server.persist.MemberRepository.MemberMigration() {
                    public void apply (com.threerings.msoy.server.persist.MemberRecord mrec)
                        throws Exception
                    {
                        if (null == load(mrec.memberId)) {
                            create(mrec.memberId);
                            log.info("Created MemberAccountRecord", "member", mrec.memberId);
                        }
                    }
                });
                } catch (Exception e) {
                    java.sql.SQLException sqle = new java.sql.SQLException("Migration failed");
                    sqle.initCause(e);
                    throw sqle;
                }
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
        return load(MemberAccountRecord.class, memberId);
    }

    /**
     * Deduct money from the specified member's money.
     * If the user does not have enough money, a NotEnoughMoneyException will be thrown.
     *
     * @return a partially filled-out MTR that should later either be passed to
     * rollbackDeduction() or filled-in and passed to storeTransaction().
     */
    public MoneyTransactionRecord deduct (
        int memberId, Currency currency, int amount, boolean allowFree)
        throws NotEnoughMoneyException
    {
        Preconditions.checkArgument(amount >= 0, "Amount to deduct must be 0 or greater.");

        ColumnExp currencyCol = MemberAccountRecord.getColumn(currency);
        Map<String, SQLExpression> fieldValues = new ImmutableMap.Builder<String, SQLExpression>()
            .put(currencyCol.getField(), new Arithmetic.Sub(currencyCol, amount))
            .build();
        Key<MemberAccountRecord> key = MemberAccountRecord.getKey(memberId);
        Where where = new Where(new And(
            new Conditionals.Equals(MemberAccountRecord.MEMBER_ID_C, memberId),
            new Conditionals.GreaterThanEquals(currencyCol, amount)));

        int count = updateLiteral(MemberAccountRecord.class, where, key, fieldValues);
        // TODO: be able to get the balance at the same time as the update, pending Depot changes
        MemberAccountRecord mar = load(MemberAccountRecord.class, key);
        if (mar == null) {
            throw new NoSuchMemberException(memberId);
        }
        int balance = mar.getAmount(currency);
        if (count == 0 && !allowFree) {
            throw new NotEnoughMoneyException(memberId, currency, amount, balance);
        }

        // Return the amount reserved, or 0 if it didn't work, but allowFree==true
        return new MoneyTransactionRecord(memberId, currency, (count == 0) ? 0 : -amount, balance);
    }

    /**
     * Deduct money from the specified member's money and immediately store the
     * transaction.
     */
    public MoneyTransactionRecord deductAndStoreTransaction (
        int memberId, Currency currency, int amount,
        TransactionType type, String description, Object subject)
        throws NotEnoughMoneyException
    {
        MoneyTransactionRecord tx = deduct(memberId, currency, amount, false);
        tx.fill(type, description, subject);
        storeTransaction(tx);
        return tx;
    }

    /**
     * Accumulate money, return a partially-populated MoneyTransactionRecord for
     * storing.
     */
    public MoneyTransactionRecord accumulate (int memberId, Currency currency, int amount)
    {
        Preconditions.checkArgument(amount >= 0, "Amount to accumulate must be 0 or greater.");

        ColumnExp currencyCol = MemberAccountRecord.getColumn(currency);
        ColumnExp currencyAccCol = MemberAccountRecord.getAccColumn(currency);
        Map<String, SQLExpression> fieldValues = new ImmutableMap.Builder<String, SQLExpression>()
            .put(currencyCol.getField(), new Arithmetic.Add(currencyCol, amount))
            .put(currencyAccCol.getField(), new Arithmetic.Add(currencyAccCol, amount))
            .build();
        Key<MemberAccountRecord> key = MemberAccountRecord.getKey(memberId);

        int count = updateLiteral(MemberAccountRecord.class, key, key, fieldValues);
        if (count == 0) {
            // the accumulate should always work, so if we mod'd 0 rows, it means there's
            // no member.
            throw new NoSuchMemberException(memberId);
        }
        // TODO: be able to get the balance at the same time as the update, pending Depot changes
        int balance = load(MemberAccountRecord.class, key).getAmount(currency);

        return new MoneyTransactionRecord(memberId, currency, amount, balance);
    }

    /**
     * Accumulate and store the specified MoneyTransaction.
     */
    public MoneyTransactionRecord accumulateAndStoreTransaction (
        int memberId, Currency currency, int amount,
        TransactionType type, String description, Object subject)
    {
        MoneyTransactionRecord tx = accumulate(memberId, currency, amount);
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
        int referenceTxId, int referenceMemberId)
    {
        MoneyTransactionRecord tx = accumulate(memberId, currency, amount);
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
        Preconditions.checkArgument(deduction.amount <= 0,
            "Only deductions can be rolled back.");
        Preconditions.checkArgument(deduction.id == 0,
            "Transaction has already been inserted!");

        ColumnExp currencyCol = MemberAccountRecord.getColumn(deduction.currency);
        Map<String, SQLExpression> fieldValues = new ImmutableMap.Builder<String, SQLExpression>()
            .put(currencyCol.getField(), new Arithmetic.Add(currencyCol, -deduction.amount))
            .build();
        Key<MemberAccountRecord> key = MemberAccountRecord.getKey(deduction.memberId);

        int count = updateLiteral(MemberAccountRecord.class, key, key, fieldValues);
        if (count == 0) {
            // This should never happen, because the deduction must have already worked!
            throw new NoSuchMemberException(deduction.memberId);
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
        int memberId, EnumSet<TransactionType> transactionTypes, Currency currency,
        int start, int count, boolean descending)
    {
        // select * from MemberAccountRecord where type = ? and transactionType in (?) 
        // and memberId=? order by timestamp
        List<QueryClause> clauses = Lists.newArrayList();
        populateSearch(clauses, memberId, transactionTypes, currency);

        clauses.add(descending ?
                    OrderBy.descending(MoneyTransactionRecord.TIMESTAMP_C) :
                    OrderBy.ascending(MoneyTransactionRecord.TIMESTAMP_C));

        if (count != Integer.MAX_VALUE) {
            clauses.add(new Limit(start, count));
        }

        return findAll(MoneyTransactionRecord.class, clauses);
    }

    public int getTransactionCount (
        int memberId, EnumSet<TransactionType> transactionTypes, Currency currency)
    {
        List<QueryClause> clauses = Lists.newArrayList();
        clauses.add(new FromOverride(MoneyTransactionRecord.class));
        populateSearch(clauses, memberId, transactionTypes, currency);
        return load(CountRecord.class, clauses).count;
    }

    public int deleteOldTransactions (final Currency currency, long maxAge)
    {
        final long oldestTimestamp = System.currentTimeMillis() - maxAge;
        return deleteAll(MoneyTransactionRecord.class, new Where(new And(
            new Conditionals.Equals(MoneyTransactionRecord.CURRENCY_C, currency),
            new Conditionals.LessThan(
                MoneyTransactionRecord.TIMESTAMP_C, new Timestamp(oldestTimestamp)))));
    }

    public List<MoneyTransactionRecord> getTransactions (final Set<Integer> ids)
    {
        return loadAll(MoneyTransactionRecord.class, ids);
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
                new Where(MoneyConfigRecord.LOCKED_C, false),
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
     * Sets the amount of bling the user has requested to cash out.  They must currently have that
     * amount of bling, and they must not already be in the process of cashing out bling.
     * 
     * @param memberId ID of the member who is making the request.
     * @param amount Amount of bling to cash out.
     * @param blingWorth The amount each bling is worth in USD at this time.
     * @return The number of records updated.  If 0, there was some error in cashing out.
     */
    public int setBlingCashOutRequested (int memberId, int amount, float blingWorth)
    {
        Where where = new Where(new And(
            new Conditionals.Equals(MemberAccountRecord.MEMBER_ID_C, memberId),
            new Conditionals.GreaterThanEquals(MemberAccountRecord.BLING_C, amount),
            new Conditionals.Equals(MemberAccountRecord.CASH_OUT_BLING_C, 0)
        ));
        return updatePartial(MemberAccountRecord.class, where,
            MemberAccountRecord.getKey(memberId),
            MemberAccountRecord.CASH_OUT_BLING, amount,
            MemberAccountRecord.CASH_OUT_BLING_WORTH, blingWorth);
    }
    
    /**
     * Indicates the bling has been cashed out, so we're no longer requesting it.
     * 
     * @param memberId ID of the member whose bling has been cashed out.
     */
    public void resetBlingCashOutRequest (int memberId)
    {
        updatePartial(MemberAccountRecord.getKey(memberId),
            MemberAccountRecord.CASH_OUT_BLING, 0,
            MemberAccountRecord.CASH_OUT_BLING_WORTH, 0f);
    }
    
    /**
     * Retrieves all member account records for members that are currently cashing out some amount
     * of their bling.
     */
    public List<MemberAccountRecord> getAccountsCashingOut ()
    {
        // select * from MemberAccountRecord where cashOutBling > 0
        return findAll(MemberAccountRecord.class, new Where(
            new Conditionals.GreaterThan(MemberAccountRecord.CASH_OUT_BLING_C, 0)));
    }
    
    /** Helper method to setup a query for a transaction history search. */
    protected void populateSearch (
        List<QueryClause> clauses, int memberId,
        EnumSet<TransactionType> transactionTypes, Currency currency)
    {
        List<SQLOperator> where = Lists.newArrayList();

        where.add(new Conditionals.Equals(MoneyTransactionRecord.MEMBER_ID_C, memberId));
        if (transactionTypes != null) {
            where.add(
                new Conditionals.In(MoneyTransactionRecord.TRANSACTION_TYPE_C, transactionTypes));
        }
        if (currency != null) {
            where.add(new Conditionals.Equals(MoneyTransactionRecord.CURRENCY_C, currency));
        }

        clauses.add(new Where(new And(where)));
    }

    @Override
    protected void getManagedRecords (final Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(MemberAccountRecord.class);
        classes.add(MoneyTransactionRecord.class);
        classes.add(MoneyConfigRecord.class);
    }
}
