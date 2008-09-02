//
// $Id$

package com.threerings.msoy.money.server.persist;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import net.jcip.annotations.NotThreadSafe;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.depot.CacheInvalidator;
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

/**
 * Depot implementation of {@link MoneyRepository}. Since depot is not thread-safe, this class is
 * not thread-safe, but all operations should be confined to a single thread (as per the contract
 * of {@link BlockingThread}.
 * 
 * This implementation manually performs optimistic locking on all records.
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 */
@Singleton
@BlockingThread
@NotThreadSafe
public final class DepotMoneyRepository extends DepotRepository
    implements MoneyRepository
{
    @Inject
    public DepotMoneyRepository (final PersistenceContext ctx)
    {
        super(ctx);
    }

    public void addHistory (final MemberAccountHistoryRecord history)
    {
        try {
            insert(history);
        } catch (final PersistenceException pe) {
            throw new RepositoryException(pe);
        }
    }

    public MemberAccountRecord getAccountById (final int memberId)
    {
        try {
            return load(MemberAccountRecord.class, memberId);
        } catch (final PersistenceException pe) {
            throw new RepositoryException(pe);
        }
    }

    public void saveAccount (final MemberAccountRecord account)
    {
        try {
            final long oldVersion = account.getVersionId();
            // Meh...
            account.versionId++;
            if (account.getVersionId() == 1) {
                insert(account);
            } else {
                final int count = updatePartial(MemberAccountRecord.class, new Where(
                    MemberAccountRecord.MEMBER_ID_C, account.getMemberId(),
                    MemberAccountRecord.VERSION_ID_C, oldVersion),
                    MemberAccountRecord.getKey(account.getMemberId()), MemberAccountRecord.BARS,
                    account.getBars(), MemberAccountRecord.COINS, account.getCoins(),
                    MemberAccountRecord.BLING, account.getBling(),
                    MemberAccountRecord.DATE_LAST_UPDATED, account.dateLastUpdated,
                    MemberAccountRecord.VERSION_ID, account.getVersionId(),
                    MemberAccountRecord.ACC_BARS, account.getAccBars(),
                    MemberAccountRecord.ACC_COINS, account.getAccCoins(),
                    MemberAccountRecord.ACC_BLING, account.getAccBling());
                if (count == 0) {
                    throw new StaleDataException("Member account record is stale: "
                        + account.getMemberId());
                }
            }
        } catch (final PersistenceException pe) {
            throw new RepositoryException(pe);
        }
    }

    public List<MemberAccountHistoryRecord> getHistory (
        final int memberId, final PersistentMoneyType type, final EnumSet<PersistentTransactionType> 
        transactionTypes, final int start, final int count, final boolean descending)
    {
        try {
            // select * from MemberAccountRecord where type = ? and transactionType in (?) 
            // and memberId=? order by timestamp
            List<QueryClause> clauses = Lists.newArrayList();
            populateSearch(clauses, memberId, type, transactionTypes);

            clauses.add(descending ?
                OrderBy.descending(MemberAccountHistoryRecord.TIMESTAMP_C) :
                OrderBy.ascending(MemberAccountHistoryRecord.TIMESTAMP_C));

            if (count != Integer.MAX_VALUE) {
                clauses.add(new Limit(start, count));
            }

            return findAll(MemberAccountHistoryRecord.class, clauses);
        } catch (final PersistenceException pe) {
            throw new RepositoryException(pe);
        }
    }

    public int deleteOldHistoryRecords (final PersistentMoneyType type, final long maxAge)
    {
        final long oldestTimestamp = System.currentTimeMillis() - maxAge;
        final Where where = new Where(new And(
            new Equals(MemberAccountHistoryRecord.TYPE_C, type), new LessThan(
                MemberAccountHistoryRecord.TIMESTAMP_C, new Timestamp(oldestTimestamp))));

        try {
            // Delete indicated records, removing the cache entries if necessary.
            return deleteAll(MemberAccountHistoryRecord.class, where,
                new CacheInvalidator.TraverseWithFilter<MemberAccountHistoryRecord>(
                    MemberAccountHistoryRecord.class) {
                    @Override
                    protected boolean testForEviction (final Serializable key,
                        final MemberAccountHistoryRecord record)
                    {
                        return record.getTimestamp().getTime() < oldestTimestamp
                            && record.getType() == type;
                    }
                });
        } catch (final PersistenceException pe) {
            throw new RepositoryException(pe);
        }
    }

    public List<MemberAccountHistoryRecord> getHistory (final Set<Integer> ids)
    {
        try {
            return findAll(MemberAccountHistoryRecord.class, new Where(
                new In(MemberAccountHistoryRecord.ID_C, ids)));
        } catch (final PersistenceException pe) {
            throw new RepositoryException(pe);
        }
    }

    /** Helper method to setup a query for a transaction history search. */
    protected void populateSearch (List<QueryClause> clauses, int memberId,
        PersistentMoneyType type, EnumSet<PersistentTransactionType> transactionTypes)
    {
        List<SQLOperator> where = Lists.newArrayList();

        where.add(new Equals(MemberAccountHistoryRecord.MEMBER_ID_C, memberId));
        if (type != null) {
            where.add(new Equals(MemberAccountHistoryRecord.TYPE_C, type));
        }
        where.add(new In(MemberAccountHistoryRecord.TRANSACTION_TYPE_C, transactionTypes));

        clauses.add(new Where(new And(where.toArray(new SQLOperator[where.size()]))));
    }

    public int getHistoryCount (
        final int memberId, final PersistentMoneyType type, final EnumSet<PersistentTransactionType>
        transactionTypes)
    {
        try {
            List<QueryClause> clauses = Lists.newArrayList();
            clauses.add(new FromOverride(MemberAccountHistoryRecord.class));
            populateSearch(clauses, memberId, type, transactionTypes);

            return load(CountRecord.class, clauses.toArray(new QueryClause[clauses.size()])).count;
        } catch (final PersistenceException pe) {
            throw new RepositoryException(pe);
        }
    }

    @Override
    protected void getManagedRecords (final Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(MemberAccountRecord.class);
        classes.add(MemberAccountHistoryRecord.class);
    }
}
