package com.threerings.msoy.server.persist;

import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.operator.Conditionals.GreaterThanEquals;
import com.samskivert.depot.operator.Conditionals.LessThanEquals;
import com.samskivert.depot.operator.Logic.And;

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
                       new Where(new And(new GreaterThanEquals(ContestRecord.STARTS_C, now),
                                         new LessThanEquals(ContestRecord.ENDS_C, now))));
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
        delete(ContestRecord.class, contestId);
    }

    @Override // from DepotRepository
    protected void getManagedRecords (final Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(ContestRecord.class);
    }
}
