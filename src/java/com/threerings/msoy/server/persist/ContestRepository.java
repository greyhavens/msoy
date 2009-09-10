//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.Ops;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.clause.Where;

import com.threerings.msoy.web.gwt.Contest;

/**
 * Manages information on our active contests.
 */
@Singleton
public class ContestRepository extends DepotRepository
{
    @Inject
    public ContestRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    /**
     * Loads all active contests.
     */
    public List<ContestRecord> loadActiveContests ()
    {
        // to make this query cacheable, round our timestamp to the current hour or so
        Timestamp now = new Timestamp(System.currentTimeMillis() & ~0x1FFFFFL);
        return findAll(ContestRecord.class,
                       new Where(Ops.and(ContestRecord.STARTS.greaterEq(now),
                                         ContestRecord.ENDS.lessEq(now))));
    }

    /**
     * Loads all contests.
     */
    public List<ContestRecord> loadContests ()
    {
        return findAll(ContestRecord.class);
    }

    /**
     * Adds a contest to the repository.
     */
    public void addContest (Contest contest)
    {
        insert(ContestRecord.fromContest(contest));
    }

    /**
     * Updates a contest in the repository.
     */
    public void updateContest (Contest contest)
    {
        update(ContestRecord.fromContest(contest));
    }

    /**
     * Removes a contest from the repository.
     */
    public void deleteContest (String contestId)
    {
        delete(ContestRecord.getKey(contestId));
    }

    @Override // from DepotRepository
    protected void getManagedRecords (final Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(ContestRecord.class);
    }
}
