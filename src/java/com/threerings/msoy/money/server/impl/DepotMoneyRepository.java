//
// $Id$

package com.threerings.msoy.money.server.impl;

import java.util.Set;

import net.jcip.annotations.NotThreadSafe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.clause.Where;
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
        throws StaleDataException
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
                    MemberAccountRecord.VERSION_ID, account.getVersionId());
                if (count == 0) {
                    throw new StaleDataException("Member account record is stale: " + account.getMemberId());
                }
            }
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
