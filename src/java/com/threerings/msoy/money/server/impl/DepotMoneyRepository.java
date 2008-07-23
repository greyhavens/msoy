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
import com.threerings.presents.annotation.BlockingThread;

/**
 * Depot implementation of {@link MoneyRepository}.  Since depot is not thread-safe, this
 * class is not thread-safe, but all operations should be confined to a single thread
 * (as per the contract of {@link BlockingThread}.
 * 
 * TODO: This implementation manually performs optimistic locking on all records.
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
            store(history);
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
            store(account);
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
