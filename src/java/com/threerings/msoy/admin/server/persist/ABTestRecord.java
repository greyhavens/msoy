//
// $Id$

package com.threerings.msoy.admin.server.persist;

import java.util.Date;
import java.sql.Timestamp;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.GeneratedValue;
import com.samskivert.depot.annotation.GenerationType;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

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
    public static final ColumnExp DESCRIPTION = colexp(_R, "description");
    public static final ColumnExp NUM_GROUPS = colexp(_R, "numGroups");
    public static final ColumnExp ONLY_NEW_VISITORS = colexp(_R, "onlyNewVisitors");
    public static final ColumnExp ENABLED = colexp(_R, "enabled");
    public static final ColumnExp STARTED = colexp(_R, "started");
    public static final ColumnExp ENDED = colexp(_R, "ended");
    public static final ColumnExp LANDING_COOKIE = colexp(_R, "landingCookie");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 9;

    /** This test's unique identifier. */
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    public int testId;

    /** The unique string identifier for this test, used to reference it when switching content. */
    @Column(length=ABTest.MAX_NAME_LENGTH, unique=true)
    public String name;

    /** More detailed description of this test. */
    @Column(length=ABTest.MAX_DESCRIPTION_LENGTH, nullable=true)
    public String description;

    /** Number of equally-sized groups for this test (2 or more) */
    @Column(defaultValue="2")
    public int numGroups;

    /** Only add visitors to a/b groups if this is their first time on whirled */
    public boolean onlyNewVisitors;

    /** Is this test being run on the site right now? */
    public boolean enabled;

    /** The date on which this test was last enabled. */
    @Column(nullable=true)
    public Timestamp started;

    /** The date on which this test was last disabled. */
    @Column(nullable=true)
    public Timestamp ended;

    /** True if the test group should be assigned to new users when they land. */
    public boolean landingCookie;

    /**
     * Build a POJO version of this Record, for use outside the persistence system.
     */
    public ABTest toABTest ()
    {
        ABTest test = new ABTest();
        test.testId = testId;
        test.name = name;
        test.description = description;
        test.numGroups = numGroups;
        test.onlyNewVisitors = onlyNewVisitors;
        test.started = (started != null) ? new Date(started.getTime()) : null;
        test.ended = (ended != null) ? new Date(ended.getTime()) : null;
        test.enabled = enabled;
        test.landingCookie = landingCookie;
        return test;
    }

    /**
     * Initializes this persistent record from the supplied runtime record. Only fields that are
     * user editable should be filled in.
     */
    public void fromABTest (ABTest test)
    {
        testId = test.testId;
        name = test.name;
        description = test.description;
        numGroups = test.numGroups;
        onlyNewVisitors = test.onlyNewVisitors;
        started = (test.started != null) ? new Timestamp(test.started.getTime()) : null;
        ended = (test.ended != null) ? new Timestamp(test.ended.getTime()) : null;
        enabled = test.enabled;
        landingCookie = test.landingCookie;
    }

    /**
     * Returns a client-consumable version of this test.
     */
    public ABTestCard toCard ()
    {
        return new ABTestCard(name, started, numGroups, onlyNewVisitors);
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
