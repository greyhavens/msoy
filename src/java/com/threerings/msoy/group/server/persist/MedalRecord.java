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

import com.threerings.msoy.group.data.all.Medal;

@Entity(indices={
    @Index(name="gropuIdIdx", fields={ MedalRecord.GROUP_ID })
})
public class MedalRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #medalId} field. */
    public static final String MEDAL_ID = "medalId";

    /** The qualified column identifier for the {@link #medalId} field. */
    public static final ColumnExp MEDAL_ID_C =
        new ColumnExp(MedalRecord.class, MEDAL_ID);

    /** The column identifier for the {@link #groupId} field. */
    public static final String GROUP_ID = "groupId";

    /** The qualified column identifier for the {@link #groupId} field. */
    public static final ColumnExp GROUP_ID_C =
        new ColumnExp(MedalRecord.class, GROUP_ID);

    /** The column identifier for the {@link #name} field. */
    public static final String NAME = "name";

    /** The qualified column identifier for the {@link #name} field. */
    public static final ColumnExp NAME_C =
        new ColumnExp(MedalRecord.class, NAME);

    /** The column identifier for the {@link #description} field. */
    public static final String DESCRIPTION = "description";

    /** The qualified column identifier for the {@link #description} field. */
    public static final ColumnExp DESCRIPTION_C =
        new ColumnExp(MedalRecord.class, DESCRIPTION);

    /** The column identifier for the {@link #iconHash} field. */
    public static final String ICON_HASH = "iconHash";

    /** The qualified column identifier for the {@link #iconHash} field. */
    public static final ColumnExp ICON_HASH_C =
        new ColumnExp(MedalRecord.class, ICON_HASH);

    /** The column identifier for the {@link #iconMimeType} field. */
    public static final String ICON_MIME_TYPE = "iconMimeType";

    /** The qualified column identifier for the {@link #iconMimeType} field. */
    public static final ColumnExp ICON_MIME_TYPE_C =
        new ColumnExp(MedalRecord.class, ICON_MIME_TYPE);
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 3;

    /** The unique id of this medal */
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    public int medalId;

    /** The group that owns this medal */
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

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link MedalRecord}
     * with the supplied key values.
     */
    public static Key<MedalRecord> getKey (int medalId)
    {
        return new Key<MedalRecord>(
                MedalRecord.class,
                new String[] { MEDAL_ID },
                new Comparable[] { medalId });
    }
    // AUTO-GENERATED: METHODS END
}
