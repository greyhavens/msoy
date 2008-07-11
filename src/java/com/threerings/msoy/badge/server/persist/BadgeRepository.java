//
// $Id$

package com.threerings.msoy.badge.server.persist;

import java.util.Set;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.clause.Where;

import com.threerings.presents.annotation.BlockingThread;

@Singleton @BlockingThread
public class BadgeRepository extends DepotRepository
{
    @Inject public BadgeRepository (PersistenceContext perCtx)
    {
        super(perCtx);
    }

    /**
     * Stores the supplied badge record in the database.
     */
    public void storeBadge (BadgeRecord badge)
        throws PersistenceException
    {
        insert(badge);
    }

    /**
     * Loads all of the specific member's badges.
     */
    public List<BadgeRecord> loadBadges (int memberId)
        throws PersistenceException
    {
        return findAll(BadgeRecord.class, new Where(BadgeRecord.MEMBER_ID_C, memberId));
    }

    @Override
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(BadgeRecord.class);
    }
}
