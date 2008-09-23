//
// $Id$

package com.threerings.msoy.money.server.persist;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.jcip.annotations.NotThreadSafe;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.DuplicateKeyException;
import com.samskivert.jdbc.depot.EntityMigration;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.clause.FromOverride;
import com.samskivert.jdbc.depot.clause.Limit;
import com.samskivert.jdbc.depot.clause.OrderBy;
import com.samskivert.jdbc.depot.clause.QueryClause;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.jdbc.depot.expression.SQLExpression;
import com.samskivert.jdbc.depot.operator.Arithmetic;
import com.samskivert.jdbc.depot.operator.SQLOperator;
import com.samskivert.jdbc.depot.operator.Conditionals.Equals;
import com.samskivert.jdbc.depot.operator.Conditionals.In;
import com.samskivert.jdbc.depot.operator.Conditionals.LessThan;
import com.samskivert.jdbc.depot.operator.Conditionals.GreaterThanEquals;
import com.samskivert.jdbc.depot.operator.Logic.And;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.server.persist.CountRecord;

import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.TransactionType;

/**
 * Interface for retrieving and persisting entities in the money service.
 *
 * @author Kyle Sampson <kyle@threerings.net>
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

        ctx.registerMigration(MemberAccountRecord.class,
            new EntityMigration.Retype(4, MemberAccountRecord.BLING));
        ctx.registerMigration(MemberAccountRecord.class,
            new EntityMigration.Retype(4, MemberAccountRecord.ACC_BLING));
        
        ctx.registerMigration(MoneyConfigRecord.class, 
            new EntityMigration.Retype(3, MoneyConfigRecord.LAST_DISTRIBUTED_BLING));
    }

    /**
     * Adds a history record for an account.
     *
     * @param history History record to update.
     */
    public void addTransaction (final MoneyTransactionRecord history)
    {
        insert(history);
    }

    /**
     * Retrieves a member's account info by member ID.
     */
    public MemberAccountRecord getAccountById (final int memberId)
    {
        MemberAccountRecord account = load(MemberAccountRecord.class, memberId);
        if (account == null) {
            account = new MemberAccountRecord(memberId);
        }
        return account;
    }

    /**
     * Adds or updates the given account.
     *
     * @param account Account to update.
     */
    public void saveAccount (final MemberAccountRecord account)
    {
        final long oldVersion = account.versionId;
        account.versionId++;
        if (oldVersion == 0) {
            insert(account);

        } else {
            // TODO: we're using updatePartial, but then we're updating everything
            // but the memberId..
            final int count = updatePartial(
                MemberAccountRecord.class, new Where(
                    MemberAccountRecord.MEMBER_ID_C, account.memberId,
                    MemberAccountRecord.VERSION_ID_C, oldVersion),
                MemberAccountRecord.getKey(account.memberId),
                MemberAccountRecord.COINS, account.coins,
                MemberAccountRecord.BARS, account.bars,
                MemberAccountRecord.BLING, account.bling,
                MemberAccountRecord.DATE_LAST_UPDATED, account.dateLastUpdated,
                MemberAccountRecord.VERSION_ID, account.versionId,
                MemberAccountRecord.ACC_BARS, account.accBars,
                MemberAccountRecord.ACC_COINS, account.accCoins,
                MemberAccountRecord.ACC_BLING, account.accBling);
            if (count == 0) {
                throw new StaleDataException("Member account record is stale: " + account.memberId);
            }
        }
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
            new Equals(MoneyTransactionRecord.CURRENCY_C, currency), new LessThan(
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
     * Exchanges some amount of bling for an equal amount of bars in a member's account.
     * 
     * @param memberId ID of the member to perform the exchange for.
     * @param amount The amount of bling (NOT centibling) to exchange for bars.
     * @return The number of records modified by this method.  If 0, indicates there is either
     * not enough bling in the member's account, or the member has no account.
     */
    public int exchangeBlingForBars (int memberId, int amount)
    {
        // update MemberAccountRecord set bars = bars + ?, bling = bling - ? where
        // memberId = ? and bling >= ?
        Where where = new Where(new And(
            new Equals(MemberAccountRecord.MEMBER_ID_C, memberId),
            new GreaterThanEquals(MemberAccountRecord.BLING_C, amount * 100)
            ));
        Map<String, SQLExpression> fieldValues = new HashMap<String, SQLExpression>();
        fieldValues.put(MemberAccountRecord.BARS, 
            new Arithmetic.Add(MemberAccountRecord.BARS_C, amount));
        fieldValues.put(MemberAccountRecord.BLING, 
            new Arithmetic.Sub(MemberAccountRecord.BLING_C, amount * 100));
        return updateLiteral(
            MemberAccountRecord.class, where,
            MemberAccountRecord.getKey(memberId),
            fieldValues);
    }

    /** Helper method to setup a query for a transaction history search. */
    protected void populateSearch (
        List<QueryClause> clauses, int memberId,
        EnumSet<TransactionType> transactionTypes, Currency currency)
    {
        List<SQLOperator> where = Lists.newArrayList();

        where.add(new Equals(MoneyTransactionRecord.MEMBER_ID_C, memberId));
        if (transactionTypes != null) {
            where.add(new In(MoneyTransactionRecord.TRANSACTION_TYPE_C, transactionTypes));
        }
        if (currency != null) {
            where.add(new Equals(MoneyTransactionRecord.CURRENCY_C, currency));
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
