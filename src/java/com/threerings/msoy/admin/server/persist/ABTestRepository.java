//
// $Id$

package com.threerings.msoy.admin.server.persist;

import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.DuplicateKeyException;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.SchemaMigration;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.expression.ValueExp;
import com.samskivert.depot.operator.Conditionals;
import com.samskivert.depot.operator.Logic;
import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.admin.gwt.ABTest;

/**
 * Maintains persistent data for a/b tests
 */
@Singleton @BlockingThread
public class ABTestRepository extends DepotRepository
{
    @Inject public ABTestRepository (PersistenceContext ctx)
    {
        super(ctx);

        ctx.registerMigration(ABTestRecord.class, new SchemaMigration.Drop(10, "enabled"));
        ctx.registerMigration(ABTestRecord.class, new SchemaMigration.Drop(10, "description"));
    }

    /**
     * Loads all test information with newest tests first.
     */
    public List<ABTestRecord> loadTests ()
    {
        return findAll(ABTestRecord.class, OrderBy.descending(ABTestRecord.TEST_ID));
    }

    /**
     * Loads a single test by the unique string identifier (name)
     */
    public ABTestRecord loadTestByName (String name)
    {
        return load(ABTestRecord.class, new Where(ABTestRecord.NAME, name));
    }

    /**
     * Inserts the supplied record into the database.
     */
    public void createTest (ABTestRecord test)
    {
        test.started = new Timestamp(System.currentTimeMillis());
        insert(test);
    }

    /**
     * Notes the group to which the specified visitor was assigned for the specified test.
     */
    public void noteABGroup (int testId, String visitorId, int group)
    {
        // load and store to avoid doing a write after the first time this method is called; it
        // will be called every time the client does whatever triggers the A/B group assignment
        if (load(ABGroupRecord.class, ABGroupRecord.getKey(testId, visitorId)) == null) {
            ABGroupRecord record = new ABGroupRecord();
            record.testId = testId;
            record.visitorId = visitorId;
            record.group = group;
            store(record);
        }

        // TODO: what's more efficient? trying to do the write and having it fail with a DKE or
        // reading first and then writing? how is this impacted by read slaves, writes always go to
        // the master, reads can talk to a slave and avoid talking to the master at all...
    }

    /**
     * Notes that the specified visitor took the specified action in the specified test.
     */
    public void noteABAction (int testId, String visitorId, String action)
    {
        try {
            ABActionRecord record = new ABActionRecord();
            record.testId = testId;
            record.visitorId = visitorId;
            record.action = action;
            insert(record);
        } catch (DuplicateKeyException dke) {
            // not a problem, they can take each action up to once
        }
    }

    /**
     * Ends any tests that are not already marked as ended and are not in the supplied set of
     * active tests. Summarizes the data for those tests and purges the raw data.
     */
    public void endAndSummarizeTests (Set<Integer> activeIds)
    {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        int mods = updateLiteral(
            ABTestRecord.class, new Where(
                new Logic.And(new Conditionals.IsNull(ABTestRecord.ENDED),
                              new Logic.Not(new Conditionals.In(ABTestRecord.TEST_ID, activeIds)))),
            null, ImmutableMap.of(ABTestRecord.ENDED, new ValueExp(now)));
        if (mods == 0) {
            // if we modded no rows, either there's nothing to summarize or another server is on it
            return;
        }

        // TODO: summarize
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(ABTestRecord.class);
        classes.add(ABGroupRecord.class);
        classes.add(ABActionRecord.class);
    }
}
