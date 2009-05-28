//
// $Id$

package com.threerings.msoy.admin.server.persist;

import java.util.Date;
import java.sql.Timestamp;

import com.google.common.base.Function;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.GeneratedValue;
import com.samskivert.depot.annotation.GenerationType;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

import com.samskivert.util.StringUtil;

import com.threerings.msoy.admin.gwt.ABTest;
import com.threerings.msoy.web.gwt.ABTestCard;

/**
 * Contains details on a single game "title" including the development and published game item ids
 * and other metrics.
 */
@Entity
public class ABTestRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<ABTestRecord> _R = ABTestRecord.class;
    public static final ColumnExp TEST_ID = colexp(_R, "testId");
    public static final ColumnExp NAME = colexp(_R, "name");
    public static final ColumnExp NUM_GROUPS = colexp(_R, "numGroups");
    public static final ColumnExp ONLY_NEW_VISITORS = colexp(_R, "onlyNewVisitors");
    public static final ColumnExp LANDING_COOKIE = colexp(_R, "landingCookie");
    public static final ColumnExp STARTED = colexp(_R, "started");
    public static final ColumnExp ENDED = colexp(_R, "ended");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 10;

    /** Converts a persistent record to a runtime record. */
    public static Function<ABTestRecord, ABTest> TO_TEST = new Function<ABTestRecord, ABTest>() {
        public ABTest apply (ABTestRecord record) {
            return record.toTest();
        }
    };

    /** This test's unique identifier. */
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    public int testId;

    /** The unique string identifier for this test. */
    @Column(length=ABTest.MAX_NAME_LENGTH, unique=true)
    public String name;

    /** Number of equally-sized groups for this test. */
    public int numGroups;

    /** Whether to only add visitors to A/B groups if this is their first time on Whirled. */
    public boolean onlyNewVisitors;

    /** True if the test group should be assigned to new users when they land. */
    public boolean landingCookie;

    /** The date on which this test was started. */
    public Timestamp started;

    /** The date on which this test was ended. */
    @Column(nullable=true)
    public Timestamp ended;

    /**
     * Returns a client-consumable version of this test.
     */
    public ABTestCard toCard ()
    {
        return new ABTestCard(name, started, numGroups, onlyNewVisitors);
    }

    /**
     * Converts this persistent record into a runtime record.
     */
    public ABTest toTest ()
    {
        return toTest(new ABTest());
    }

    /**
     * Initializes the supplied runtime record with info from this persistent record.
     */
    public <T extends ABTest> T toTest (T test)
    {
        test.testId = testId;
        test.name = name;
        test.numGroups = numGroups;
        test.onlyNewVisitors = onlyNewVisitors;
        test.landingCookie = landingCookie;
        test.started = (started != null) ? new Date(started.getTime()) : null;
        test.ended = (ended != null) ? new Date(ended.getTime()) : null;
        return test;
    }

    @Override // from Object
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link ABTestRecord}
     * with the supplied key values.
     */
    public static Key<ABTestRecord> getKey (int testId)
    {
        return new Key<ABTestRecord>(
                ABTestRecord.class,
                new ColumnExp[] { TEST_ID },
                new Comparable[] { testId });
    }
    // AUTO-GENERATED: METHODS END
}
