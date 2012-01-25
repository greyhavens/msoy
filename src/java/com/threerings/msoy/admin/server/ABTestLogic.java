//
// $Id$

package com.threerings.msoy.admin.server;

import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.ExpiringReference;
import com.samskivert.util.Lifecycle;

import com.samskivert.depot.DuplicateKeyException;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.admin.gwt.ABTestSummary;
import com.threerings.msoy.admin.server.persist.ABTestRecord;
import com.threerings.msoy.admin.server.persist.ABTestRepository;
import com.threerings.msoy.data.all.VisitorInfo;
import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.web.gwt.ABTestCard;

import static com.threerings.msoy.Log.log;

/**
 * Provides A/B testing services.
 */
@Singleton @BlockingThread
public class ABTestLogic
    implements Lifecycle.InitComponent
{
    @Inject public ABTestLogic (Lifecycle cycle)
    {
        cycle.addComponent(this);
    }

    // from Lifecycle.InitComponent
    public void init ()
    {
        // register new tests here
        // registerTest("NAME", 4 /* num groups */, false /* only new */, false /* landing */);
        registerTest("2010 05 register (1) room (2)", 2, true, true);
        registerTest("2012 01 DJ landings, dark (1) light (2)", 2, false, true);

        // mark any tests that are no longer registered as ended, purge really old tests
        Set<Integer> activeIds = Sets.newHashSet();
        for (ABTestRecord record : _tests.values()) {
            activeIds.add(record.testId);
        }
        _testRepo.endAndSummarizeTests(activeIds);
    }

    /**
     * Returns all active tests that require a cookie to be assigned on landing.
     */
    public Iterable<ABTestCard> getTestsWithLandingCookies ()
    {
        return Iterables.transform(Iterables.filter(_tests.values(), IS_LANDING), TO_CARD);
    }

    /**
     * Return the A/B test group that a member or visitor belongs to for a given A/B test,
     * generated psudo-randomly based on their tracking ID and the test name.  If the visitor is
     * not eligible for the A/B test, return < 0.
     *
     * @param test the name of the test for which a group is desired.
     * @param logEvent if true, track that this visitor was added to this group.
     *
     * @return the A/B group the visitor has been assigned to (where 1 <= group <= numGroups), or <
     * 0 for no group.
     */
    public int getABTestGroup (String test, VisitorInfo info, boolean logEvent)
    {
        if (info == null) { // sanity check
            log.warning("Missing visitor info", "test", test, "info", info, "logEvent", logEvent);
            return -1;
        }

        ABTestRecord trec = _tests.get(test);
        if (trec == null) {
            log.warning("Requested group for non-existent test", "test", test, "info", info);
            return -1;
        }

        // generate the group number based on trackingID + testName
        int group = trec.toCard().getGroup(info);

        // log an event to say the group was assigned if requested
        if (logEvent && group >= 0) {
            _testRepo.noteABGroup(trec.testId, info.id, group);
        }

        return group;
    }

    /**
     * Notes that the specified visitor took the specified action in the specified test.
     */
    public void trackTestAction (String test, String action, VisitorInfo info)
    {
        if (info == null) { // sanity check
            log.warning("Missing visitor info", "test", test, "action", action, "info", info);
            return;
        }

        ABTestRecord trec = _tests.get(test);
        if (trec == null) {
            log.warning("Requested to track action for non-existent test", "test", test,
                        "action", action, "info", info);
            return;
        }
        _testRepo.noteABAction(trec.testId, info.id, action);
    }

    /**
     * Returns the summary for the specified test.
     */
    public ABTestSummary getSummary (int testId)
    {
        // because we resummarize on every load and summarizing is expensive, we maintain a cache
        ExpiringReference<ABTestSummary> ref = _sums.get(testId);
        ABTestSummary sum = (ref == null) ? null : ref.getValue();
        if (sum == null) {
            sum = _testRepo.loadSummary(testId);
            if (sum != null) {
                _sums.put(testId, ExpiringReference.create(sum, SUM_EXPIRE_TIME));
            }
        }
        return sum;
    }

    protected void registerTest (String name, int numGroups, boolean onlyNewVisitors,
                                 boolean landingCookie)
    {
        // check whether or not we've got a test of the specified name
        ABTestRecord record = _testRepo.loadTestByName(name);
        if (record != null) {
            if (numGroups != record.numGroups || onlyNewVisitors != record.onlyNewVisitors ||
                landingCookie != record.landingCookie) {
                throw new IllegalArgumentException("Conflicing test " + record);
            }
        } else {
            record = new ABTestRecord();
            record.name = name;
            record.numGroups = numGroups;
            record.onlyNewVisitors = onlyNewVisitors;
            record.landingCookie = landingCookie;
            try {
                _testRepo.createTest(record);
                log.info("Starting A/B test " + record);
            } catch (DuplicateKeyException dke) {
                // not a problem, some other server created the test before us
                record = _testRepo.loadTestByName(record.name);
            }
        }
        if (record.testId == 0) {
            throw new IllegalStateException("Test has no id? " + record);
        }
        _tests.put(record.name, record);
    }

    protected Map<String, ABTestRecord> _tests = Maps.newHashMap();
    protected Map<Integer, ExpiringReference<ABTestSummary>> _sums = Maps.newHashMap();

    @Inject protected ABTestRepository _testRepo;
    @Inject protected MsoyEventLogger _eventLog;

    protected static Predicate<ABTestRecord> IS_LANDING = new Predicate<ABTestRecord>() {
        public boolean apply (ABTestRecord record) {
            return record.landingCookie;
        }
    };
    protected static Function<ABTestRecord, ABTestCard> TO_CARD =
        new Function<ABTestRecord, ABTestCard>() {
        public ABTestCard apply (ABTestRecord record) {
            return record.toCard();
        }
    };

    protected static final long SUM_EXPIRE_TIME = 5 * 60 * 1000L;
}
