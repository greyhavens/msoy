//
// $Id: $

package com.threerings.msoy.group.server.persist;

import com.google.common.base.Function;
import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

/** Enumerates the avatars available to new players of a theme. */
@Entity
public class ThemeAvatarLineupRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<ThemeAvatarLineupRecord> _R = ThemeAvatarLineupRecord.class;
    public static final ColumnExp GROUP_ID = colexp(_R, "groupId");
    public static final ColumnExp CATALOG_ID = colexp(_R, "catalogId");
    public static final ColumnExp GENDER = colexp(_R, "gender");
    // AUTO-GENERATED: FIELDS END

    public static final byte GENDER_MALE = 1;
    public static final byte GENDER_FEMALE = 2;
    public static final byte GENDER_OTHER = 3;

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** Provides the {@link #catalogId} of a record. */
    public static final Function<ThemeAvatarLineupRecord, Integer> GET_CATALOG_ID =
        new Function<ThemeAvatarLineupRecord, Integer>() {
            public Integer apply (ThemeAvatarLineupRecord record) {
                return record.catalogId;
            }
    };

    /** The groupId of the theme for which we're enumerating avatars. */
    @Id
    public int groupId;

    /** The catalogId reference. */
    @Id
    public int catalogId;

    /** One of {@link #GENDER_MALE}, {@link #GENDER_FEMALE}, {@link #GENDER_OTHER}. */
    public byte gender;

    public ThemeAvatarLineupRecord ()
    {
    }

    public ThemeAvatarLineupRecord (int groupId, int catalogId, byte gender)
    {
        this.groupId = groupId;
        this.catalogId = catalogId;
        this.gender = gender;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link ThemeAvatarLineupRecord}
     * with the supplied key values.
     */
    public static Key<ThemeAvatarLineupRecord> getKey (int groupId, int catalogId)
    {
        return new Key<ThemeAvatarLineupRecord>(
                ThemeAvatarLineupRecord.class,
                new ColumnExp[] { GROUP_ID, CATALOG_ID },
                new Comparable[] { groupId, catalogId });
    }
    // AUTO-GENERATED: METHODS END
}
