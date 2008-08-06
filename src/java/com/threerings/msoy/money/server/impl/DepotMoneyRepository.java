//
// $Id$

package com.threerings.msoy.money.server.impl;

import java.util.List;
import java.util.Set;

import net.jcip.annotations.NotThreadSafe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.clause.Limit;
import com.samskivert.jdbc.depot.clause.OrderBy;
import com.samskivert.jdbc.depot.clause.QueryClause;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.jdbc.depot.operator.Conditionals.Equals;
import com.samskivert.jdbc.depot.operator.Logic.And;
import com.threerings.msoy.money.server.MoneyType;
import com.threerings.presents.annotation.BlockingThread;

/**
 * Depot implementation of {@link MoneyRepository}.  Since depot is not thread-safe, this
 * class is not thread-safe, but all operations should be confined to a single thread
 * (as per the contract of {@link BlockingThread}.
 * 
 * This implementation manually performs optimistic locking on all records.
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 */
@Singleton 
@BlockingThread
@NotThreadSafe
final class DepotMoneyRepository extends DepotRepository
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
                final int count = updatePartial(MemberAccountRecord.class,
                    new Where(MemberAccountRecord.MEMBER_ID_C, account.getMemberId(), 
                        MemberAccountRecord.VERSION_ID_C, oldVersion),
                    MemberAccountRecord.getKey(account.getMemberId()),
                    MemberAccountRecord.BARS, account.getBars(),
                    MemberAccountRecord.COINS, account.getCoins(),
                    MemberAccountRecord.BLING, account.getBling(),
                    MemberAccountRecord.DATE_LAST_UPDATED, account.dateLastUpdated,
                    MemberAccountRecord.VERSION_ID, account.getVersionId(),
                    MemberAccountRecord.ACC_BARS, account.getAccBars(),
                    MemberAccountRecord.ACC_COINS, account.getAccCoins(),
                    MemberAccountRecord.ACC_BLING, account.getAccBling());
                if (count == 0) {
                    throw new StaleDataException("Member account record is stale: " + account.getMemberId());
                }
            }
        } catch (final PersistenceException pe) {
            throw new RepositoryException(pe);
        }
    }
    
    public List<MemberAccountHistoryRecord> getHistory (final int memberId, final MoneyType type, 
        final int start, final int count, final boolean descending)
    {
        try {
            // select * from MemberAccountRecord where type in (?) and memberId=? order by timestamp
            final QueryClause where = type == null ?
                new Where(new Equals(MemberAccountHistoryRecord.MEMBER_ID_C, memberId)) :
                new Where(new And(
                    new Equals(MemberAccountHistoryRecord.TYPE_C, type),
                    new Equals(MemberAccountHistoryRecord.MEMBER_ID_C, memberId)
                ));
            final QueryClause orderBy = descending ?
                    OrderBy.descending(MemberAccountHistoryRecord.TIMESTAMP_C) :
                    OrderBy.ascending(MemberAccountHistoryRecord.TIMESTAMP_C);
            final QueryClause limit = new Limit(start, count);
            
            // Only include the limit clause if we're not getting all the values.
            return count == Integer.MAX_VALUE ?
                findAll(MemberAccountHistoryRecord.class, where, orderBy) :
                findAll(MemberAccountHistoryRecord.class, where, orderBy, limit);
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
