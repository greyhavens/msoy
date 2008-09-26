//
// $Id$

package com.threerings.msoy.admin.server.persist;

import java.util.Date;
import java.sql.Timestamp;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Column;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.GeneratedValue;
import com.samskivert.jdbc.depot.annotation.GenerationType;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.annotation.TableGenerator;
import com.samskivert.jdbc.depot.expression.ColumnExp;
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
    /** The column identifier for the {@link #abTestId} field. */
    public static final String AB_TEST_ID = "abTestId";

    /** The qualified column identifier for the {@link #abTestId} field. */
    public static final ColumnExp AB_TEST_ID_C =
        new ColumnExp(ABTestRecord.class, AB_TEST_ID);

    /** The column identifier for the {@link #name} field. */
    public static final String NAME = "name";

    /** The qualified column identifier for the {@link #name} field. */
    public static final ColumnExp NAME_C =
        new ColumnExp(ABTestRecord.class, NAME);

    /** The column identifier for the {@link #description} field. */
    public static final String DESCRIPTION = "description";

    /** The qualified column identifier for the {@link #description} field. */
    public static final ColumnExp DESCRIPTION_C =
        new ColumnExp(ABTestRecord.class, DESCRIPTION);

    /** The column identifier for the {@link #numGroups} field. */
    public static final String NUM_GROUPS = "numGroups";

    /** The qualified column identifier for the {@link #numGroups} field. */
    public static final ColumnExp NUM_GROUPS_C =
        new ColumnExp(ABTestRecord.class, NUM_GROUPS);

    /** The column identifier for the {@link #onlyNewVisitors} field. */
    public static final String ONLY_NEW_VISITORS = "onlyNewVisitors";

    /** The qualified column identifier for the {@link #onlyNewVisitors} field. */
    public static final ColumnExp ONLY_NEW_VISITORS_C =
        new ColumnExp(ABTestRecord.class, ONLY_NEW_VISITORS);

    /** The column identifier for the {@link #enabled} field. */
    public static final String ENABLED = "enabled";

    /** The qualified column identifier for the {@link #enabled} field. */
    public static final ColumnExp ENABLED_C =
        new ColumnExp(ABTestRecord.class, ENABLED);

    /** The column identifier for the {@link #started} field. */
    public static final String STARTED = "started";

    /** The qualified column identifier for the {@link #started} field. */
    public static final ColumnExp STARTED_C =
        new ColumnExp(ABTestRecord.class, STARTED);

    /** The column identifier for the {@link #ended} field. */
    public static final String ENDED = "ended";

    /** The qualified column identifier for the {@link #ended} field. */
    public static final ColumnExp ENDED_C =
        new ColumnExp(ABTestRecord.class, ENDED);
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
    public static final int SCHEMA_VERSION = 5;

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
                new String[] { AB_TEST_ID },
                new Comparable[] { abTestId });
    }
    // AUTO-GENERATED: METHODS END
}
