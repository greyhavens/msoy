//
// $Id$

package com.threerings.msoy.room.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Holds information about a room property.
 */
@Entity
public class RoomPropertyRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<RoomPropertyRecord> _R = RoomPropertyRecord.class;
    public static final ColumnExp OWNER_ID = colexp(_R, "ownerId");
    public static final ColumnExp SCENE_ID = colexp(_R, "sceneId");
    public static final ColumnExp NAME = colexp(_R, "name");
    public static final ColumnExp VALUE = colexp(_R, "value");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent
     * object in a way that will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** The id of the game this property belongs to. */
    @Id public int ownerId;

    /** The id of the scene this property applies to. */
    @Id public int sceneId;

    /** The unique name of the property. */
    @Id public String name;

    /** The serialized property value. */
    @Column(length=4096) public byte[] value;

    /** Creates a new property record ready to be populated from the database. */
    public RoomPropertyRecord ()
    {
    }

    /** Creates a new property record with given values. */
    public RoomPropertyRecord (int ownerId, int sceneId, String name, byte[] value)
    {
        this.ownerId = ownerId;
        this.sceneId = sceneId;
        this.name = name;
        this.value = value;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link RoomPropertyRecord}
     * with the supplied key values.
     */
    public static Key<RoomPropertyRecord> getKey (int ownerId, int sceneId, String name)
    {
        return new Key<RoomPropertyRecord>(
                RoomPropertyRecord.class,
                new ColumnExp[] { OWNER_ID, SCENE_ID, NAME },
                new Comparable[] { ownerId, sceneId, name });
    }
    // AUTO-GENERATED: METHODS END
}
