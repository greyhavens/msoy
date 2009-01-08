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
import com.samskivert.depot.annotation.TableGenerator;
import com.samskivert.depot.expression.ColumnExp;
import com.threerings.msoy.admin.gwt.ABTest;

/**
 * Contains details on a single game "title" including the development and published game item ids
 * and other metrics.
 */
@Entity
@TableGenerator(name="abTest", pkColumnValue="ABTEST_ID")
public class ABTestRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<ABTestRecord> _R = ABTestRecord.class;
    public static final ColumnExp AB_TEST_ID = colexp(_R, "abTestId");
    public static final ColumnExp NAME = colexp(_R, "name");
    public static final ColumnExp DESCRIPTION = colexp(_R, "description");
    public static final ColumnExp NUM_GROUPS = colexp(_R, "numGroups");
    public static final ColumnExp ONLY_NEW_VISITORS = colexp(_R, "onlyNewVisitors");
    public static final ColumnExp ENABLED = colexp(_R, "enabled");
    public static final ColumnExp STARTED = colexp(_R, "started");
    public static final ColumnExp ENDED = colexp(_R, "ended");
    // AUTO-GENERATED: FIELDS END

    /** This item's unique identifier. <em>Note:</em> this identifier is not globally unique among
     * all digital items. Each type of item has its own identifier space. */
    @Id
    @GeneratedValue(generator="itemId", strategy=GenerationType.TABLE, allocationSize=1)
    public int abTestId;

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

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 7;

    /**
     * Build a POJO version of this Record, for use outside the persistence system.
     */
    public ABTest toABTest ()
    {
        ABTest test = new ABTest();
        test.abTestId = abTestId;
        test.name = name;
        test.description = description;
        test.numGroups = numGroups;
        test.onlyNewVisitors = onlyNewVisitors;
        test.started = (started != null) ? new Date(started.getTime()) : null;
        test.ended = (ended != null) ? new Date(ended.getTime()) : null;
        test.enabled = enabled;
        return test;
    }

    /**
     * Initializes this persistent record from the supplied runtime record. Only fields that are
     * user editable should be filled in.
     */
    public void fromABTest (ABTest test)
    {
        abTestId = test.abTestId;
        name = test.name;
        description = test.description;
        numGroups = test.numGroups;
        onlyNewVisitors = test.onlyNewVisitors;
        started = (test.started != null) ? new Timestamp(test.started.getTime()) : null;
        ended = (test.ended != null) ? new Timestamp(test.ended.getTime()) : null;
        enabled = test.enabled;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link ABTestRecord}
     * with the supplied key values.
     */
    public static Key<ABTestRecord> getKey (int abTestId)
    {
        return new Key<ABTestRecord>(
                ABTestRecord.class,
                new ColumnExp[] { AB_TEST_ID },
                new Comparable[] { abTestId });
    }
    // AUTO-GENERATED: METHODS END
}
