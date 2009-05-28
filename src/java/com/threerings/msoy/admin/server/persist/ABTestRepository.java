//
// $Id$

package com.threerings.msoy.admin.server.persist;

import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.DuplicateKeyException;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.SchemaMigration;
import com.samskivert.depot.annotation.Computed;
import com.samskivert.depot.clause.FromOverride;
import com.samskivert.depot.clause.GroupBy;
import com.samskivert.depot.clause.Join;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.expression.LiteralExp;
import com.samskivert.depot.expression.ValueExp;
import com.samskivert.depot.operator.And;
import com.samskivert.depot.operator.Equals;
import com.samskivert.depot.operator.GreaterThan;
import com.samskivert.depot.operator.In;
import com.samskivert.depot.operator.IsNull;
import com.samskivert.depot.operator.Not;
import com.samskivert.depot.operator.NotEquals;
import com.samskivert.depot.operator.Sub;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.admin.gwt.ABTest;
import com.threerings.msoy.admin.gwt.ABTestSummary;
import com.threerings.msoy.server.persist.EntryVectorRecord;
import com.threerings.msoy.server.persist.MemberRecord;

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
     * Loads a summary of the supplied test. If the test is still running, the current summary will
     * first be regenerated.
     *
     * @return the summary record or null if no test exists with the supplied id.
     */
    public ABTestSummary loadSummary (int testId)
    {
        ABTestRecord record = load(ABTestRecord.class, testId);
        if (record == null) {
            return null;
        }

        // if the test is still running, then resummarize before loading our data
        if (record.ended == null) {
            summarizeTest(testId);
        }

        ABTestSummary sum = record.toTest(new ABTestSummary());
        sum.groups = Lists.newArrayList();

        Multimap<Integer, ABActionSummaryRecord> actions = HashMultimap.create();
        for (ABActionSummaryRecord arec : findAll(
                 ABActionSummaryRecord.class, new Where(ABActionSummaryRecord.TEST_ID, testId))) {
            actions.put(arec.group, arec);
        }

        for (ABGroupSummaryRecord gsum : findAll(
                 ABGroupSummaryRecord.class, new Where(ABGroupSummaryRecord.TEST_ID, testId))) {
            ABTestSummary.Group group = new ABTestSummary.Group();
            group.group = gsum.group;
            group.assigned = gsum.assigned;
            group.registered = gsum.registered;
            group.retained = gsum.retained;
            group.actions = Maps.newHashMap();
            for (ABActionSummaryRecord arec : actions.get(group.group)) {
                group.actions.put(arec.action, arec.takers);
            }
        }

        return sum;
    }

    /**
     * Summarizes the results of the specified test. This is expensive.
     */
    public void summarizeTest (int testId)
    {
        // first determine the number of visitors assigned to the test groups
        IntMap<ABGroupSummaryRecord> groups = IntMaps.newHashIntMap();
        for (GroupCountRecord rec : findAll(
                 GroupCountRecord.class, new FromOverride(ABGroupRecord.class),
                 new GroupBy(ABGroupRecord.GROUP),
                 new Where(ABGroupRecord.TEST_ID, testId))) {
            ABGroupSummaryRecord sumrec = new ABGroupSummaryRecord();
            sumrec.testId = testId;
            sumrec.group = rec.group;
            sumrec.assigned = rec.count;
            groups.put(sumrec.group, sumrec);
        }

        // now determine how many of those members registered
        for (GroupCountRecord rec : findAll(
                 GroupCountRecord.class, new FromOverride(ABGroupRecord.class),
                 new Join(ABGroupRecord.VISITOR_ID, EntryVectorRecord.VISITOR_ID),
                 new GroupBy(ABGroupRecord.GROUP),
                 new Where(new And(new Equals(ABGroupRecord.TEST_ID, testId),
                                   new NotEquals(EntryVectorRecord.MEMBER_ID, 0))))) {
            groups.get(rec.group).registered = rec.count;
        }

        // now determine how many of those members were retained
        for (GroupCountRecord rec : findAll(
                 GroupCountRecord.class, new FromOverride(ABGroupRecord.class),
                 new Join(ABGroupRecord.VISITOR_ID, EntryVectorRecord.VISITOR_ID),
                 new Join(EntryVectorRecord.MEMBER_ID, MemberRecord.MEMBER_ID),
                 new GroupBy(ABGroupRecord.GROUP),
                 new Where(new And(new Equals(ABGroupRecord.TEST_ID, testId),
                                   new GreaterThan(new Sub(MemberRecord.LAST_SESSION,
                                                           new LiteralExp("interval '7 day'")),
                                                   MemberRecord.CREATED))))) {
            groups.get(rec.group).retained = rec.count;
        }

        // now store the group summary records
        for (ABGroupSummaryRecord sumrec : groups.values()) {
            store(sumrec);
        }

        // lastly summarize the actions
        for (ActionCountRecord rec : findAll(
                 ActionCountRecord.class, new FromOverride(ABActionRecord.class),
                 new Join(ABActionRecord.VISITOR_ID, ABGroupRecord.VISITOR_ID),
                 new GroupBy(ABGroupRecord.GROUP, ABActionRecord.ACTION),
                 new Where(ABActionRecord.TEST_ID, testId))) {
            ABActionSummaryRecord sumrec = new ABActionSummaryRecord();
            sumrec.testId = testId;
            sumrec.group = rec.group;
            sumrec.action = rec.action;
            sumrec.takers = rec.count;
            store(sumrec);
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
                new And(new IsNull(ABTestRecord.ENDED),
                        new Not(new In(ABTestRecord.TEST_ID, activeIds)))),
            null, ImmutableMap.of(ABTestRecord.ENDED, new ValueExp(now)));
        if (mods == 0) {
            // if we modded no rows, either there's nothing to summarize or another server is on it
            return;
        }

        // summarize and purge the tests that we just ended
        for (ABTestRecord rec : findAll(ABTestRecord.class, new Where(ABTestRecord.ENDED, now))) {
            summarizeTest(rec.testId);
            deleteAll(ABGroupRecord.class, new Where(ABGroupRecord.TEST_ID, rec.testId), null);
            deleteAll(ABActionRecord.class, new Where(ABActionRecord.TEST_ID, rec.testId), null);
        }
    }

    @Computed
    protected static class GroupCountRecord extends PersistentRecord {
        public int group;
        @Computed(fieldDefinition="count(*)") public int count;
    }

    @Computed
    protected static class ActionCountRecord extends PersistentRecord {
        public int group;
        public String action;
        @Computed(fieldDefinition="count(*)") public int count;
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(ABTestRecord.class);
        classes.add(ABGroupRecord.class);
        classes.add(ABGroupSummaryRecord.class);
        classes.add(ABActionRecord.class);
        classes.add(ABActionSummaryRecord.class);
    }
}
