//
// $Id: $

package com.threerings.msoy.group.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

/**
 *  Contains data specific to the theme aspect of a group.
 */
@Entity
public class ThemeRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<ThemeRecord> _R = ThemeRecord.class;
    public static final ColumnExp GROUP_ID = colexp(_R, "groupId");
    public static final ColumnExp PLAY_ON_ENTER = colexp(_R, "playOnEnter");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 2;

    /** The groupId of this theme. */
    @Id
    public int groupId;

    /** Whether or not to start this theme group's associated AVRG upon entering a themed room. */
    public boolean playOnEnter;

    public ThemeRecord ()
    {
    }

    public ThemeRecord (int groupId)
    {
        this.groupId = groupId;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link ThemeRecord}
     * with the supplied key values.
     */
    public static Key<ThemeRecord> getKey (int groupId)
    {
        return new Key<ThemeRecord>(
                ThemeRecord.class,
                new ColumnExp[] { GROUP_ID },
                new Comparable[] { groupId });
    }
    // AUTO-GENERATED: METHODS END
}
