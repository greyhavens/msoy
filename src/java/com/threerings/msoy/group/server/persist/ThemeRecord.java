//
// $Id: $

package com.threerings.msoy.group.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;
import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.Theme;

/**
 *  Contains data specific to the theme aspect of a group.
 */
@Entity
public class ThemeRecord extends PersistentRecord
{
    public static final int DEFAULT_BACKGROUND_COLOR = 0xFFFFFF;

    // AUTO-GENERATED: FIELDS START
    public static final Class<ThemeRecord> _R = ThemeRecord.class;
    public static final ColumnExp GROUP_ID = colexp(_R, "groupId");
    public static final ColumnExp PLAY_ON_ENTER = colexp(_R, "playOnEnter");
    public static final ColumnExp LOGO_MEDIA_HASH = colexp(_R, "logoMediaHash");
    public static final ColumnExp LOGO_MIME_TYPE = colexp(_R, "logoMimeType");
    public static final ColumnExp LOGO_MEDIA_CONSTRAINT = colexp(_R, "logoMediaConstraint");
    public static final ColumnExp BACKGROUND_COLOR = colexp(_R, "backgroundColor");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 4;

    /** The groupId of this theme. */
    @Id
    public int groupId;

    /** Whether or not to start this theme group's associated AVRG upon entering a themed room. */
    public boolean playOnEnter;

    /** A hash code identifying the media for this theme's logo. */
    @Column(nullable=true)
    public byte[] logoMediaHash;

    /** The MIME type of this theme's logo. */
    public byte logoMimeType;

    /** The constraint for the logo image. */
    public byte logoMediaConstraint;

    /** The background colour of the main Whirled UI. */
    public int backgroundColor = DEFAULT_BACKGROUND_COLOR;

    public ThemeRecord ()
    {
    }

    public ThemeRecord (int groupId)
    {
        this.groupId = groupId;
    }

    /**
     * Creates a Theme of this record.
     */
    public Theme toTheme (GroupName group)
    {
        return new Theme(group, playOnEnter, toLogo(), backgroundColor);
    }

    /**
     * Creates a MediaDesc of the theme logo, or returns null if there is none.
     */
    public MediaDesc toLogo ()
    {
        if (logoMediaHash == null) {
            return null;
        }
        return new MediaDesc(logoMediaHash, logoMimeType, logoMediaConstraint);
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
