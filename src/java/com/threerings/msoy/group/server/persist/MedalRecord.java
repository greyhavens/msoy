//
// $Id$

package com.threerings.msoy.group.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.GeneratedValue;
import com.samskivert.depot.annotation.GenerationType;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.expression.ColumnExp;

import com.google.common.base.Function;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.group.data.all.Medal;

@Entity
public class MedalRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<MedalRecord> _R = MedalRecord.class;
    public static final ColumnExp MEDAL_ID = colexp(_R, "medalId");
    public static final ColumnExp GROUP_ID = colexp(_R, "groupId");
    public static final ColumnExp NAME = colexp(_R, "name");
    public static final ColumnExp DESCRIPTION = colexp(_R, "description");
    public static final ColumnExp ICON_HASH = colexp(_R, "iconHash");
    public static final ColumnExp ICON_MIME_TYPE = colexp(_R, "iconMimeType");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 3;

    /** Converts a persistent record into a {@link Medal}. */
    public static final Function<MedalRecord, Medal> TO_MEDAL = new Function<MedalRecord, Medal>() {
        public Medal apply (MedalRecord record) {
            return record.toMedal();
        }
    };

    /** Converts a persistent record into an Integer representing the medal id */
    public static final Function<MedalRecord, Integer> TO_MEDAL_ID =
        new Function<MedalRecord, Integer>() {
            public Integer apply (MedalRecord record) {
                return record.medalId;
            }
        };

    /** The unique id of this medal */
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    public int medalId;

    /** The group that owns this medal */
    @Index(name="gropuIdIdx")
    public int groupId;

    /** The name of the medal. */
    @Column(length=Medal.MAX_NAME_LENGTH)
    public String name;

    /** The description of the medal */
    @Column(length=Medal.MAX_DESCRIPTION_LENGTH)
    public String description;

    /** The media hash of the medal icon.  We don't store the constraint because they're all
     * 80x60 */
    public byte[] iconHash;

    /** The mime type of the medal icon. */
    public byte iconMimeType;

    public Medal toMedal ()
    {
        Medal medal = new Medal();
        medal.medalId = medalId;
        medal.groupId = groupId;
        medal.name = name;
        medal.description = description;
        medal.icon = createIconMedia();
        return medal;
    }

    public MediaDesc createIconMedia()
    {
        // Images are larger than half-thumbnail, so we can't pretend they're not constrained at all
        return new MediaDesc(iconHash, iconMimeType, MediaDesc.HALF_HORIZONTALLY_CONSTRAINED);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link MedalRecord}
     * with the supplied key values.
     */
    public static Key<MedalRecord> getKey (int medalId)
    {
        return new Key<MedalRecord>(
                MedalRecord.class,
                new ColumnExp[] { MEDAL_ID },
                new Comparable[] { medalId });
    }
    // AUTO-GENERATED: METHODS END
}
