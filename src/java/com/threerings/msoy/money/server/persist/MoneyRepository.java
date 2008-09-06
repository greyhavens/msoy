//
// $Id$

package com.threerings.msoy.money.server.persist;

import java.sql.Timestamp;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import net.jcip.annotations.NotThreadSafe;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.clause.FromOverride;
import com.samskivert.jdbc.depot.clause.Limit;
import com.samskivert.jdbc.depot.clause.OrderBy;
import com.samskivert.jdbc.depot.clause.QueryClause;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.jdbc.depot.operator.SQLOperator;
import com.samskivert.jdbc.depot.operator.Conditionals.Equals;
import com.samskivert.jdbc.depot.operator.Conditionals.In;
import com.samskivert.jdbc.depot.operator.Conditionals.LessThan;
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
        return load(MemberAccountRecord.class, memberId);
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
        int memberId, Currency currency, EnumSet<TransactionType> transactionTypes,
        int start, int count, boolean descending)
    {
        // select * from MemberAccountRecord where type = ? and transactionType in (?) 
        // and memberId=? order by timestamp
        List<QueryClause> clauses = Lists.newArrayList();
        populateSearch(clauses, memberId, currency, transactionTypes);

        clauses.add(descending ?
                    OrderBy.descending(MoneyTransactionRecord.TIMESTAMP_C) :
                    OrderBy.ascending(MoneyTransactionRecord.TIMESTAMP_C));

        if (count != Integer.MAX_VALUE) {
            clauses.add(new Limit(start, count));
        }

        return findAll(MoneyTransactionRecord.class, clauses);
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

    public int getHistoryCount (int memberId, Currency currency,
                                EnumSet<TransactionType> transactionTypes)
    {
        List<QueryClause> clauses = Lists.newArrayList();
        clauses.add(new FromOverride(MoneyTransactionRecord.class));
        populateSearch(clauses, memberId, currency, transactionTypes);
        return load(CountRecord.class, clauses).count;
    }

    /** Helper method to setup a query for a transaction history search. */
    protected void populateSearch (
        List<QueryClause> clauses, int memberId, Currency currency,
        EnumSet<TransactionType> transactionTypes)
    {
        List<SQLOperator> where = Lists.newArrayList();

        where.add(new Equals(MoneyTransactionRecord.MEMBER_ID_C, memberId));
        if (currency != null) {
            where.add(new Equals(MoneyTransactionRecord.CURRENCY_C, currency));
        }
        if (transactionTypes != null) {
            where.add(new In(MoneyTransactionRecord.TRANSACTION_TYPE_C, transactionTypes));
        }

        clauses.add(new Where(new And(where)));
    }

    @Override
    protected void getManagedRecords (final Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(MemberAccountRecord.class);
        classes.add(MoneyTransactionRecord.class);
    }
}
