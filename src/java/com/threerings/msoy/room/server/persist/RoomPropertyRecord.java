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
    /** The column identifier for the {@link #ownerId} field. */
    public static final String OWNER_ID = "ownerId";

    /** The qualified column identifier for the {@link #ownerId} field. */
    public static final ColumnExp OWNER_ID_C =
        new ColumnExp(RoomPropertyRecord.class, OWNER_ID);

    /** The column identifier for the {@link #sceneId} field. */
    public static final String SCENE_ID = "sceneId";

    /** The qualified column identifier for the {@link #sceneId} field. */
    public static final ColumnExp SCENE_ID_C =
        new ColumnExp(RoomPropertyRecord.class, SCENE_ID);

    /** The column identifier for the {@link #name} field. */
    public static final String NAME = "name";

    /** The qualified column identifier for the {@link #name} field. */
    public static final ColumnExp NAME_C =
        new ColumnExp(RoomPropertyRecord.class, NAME);

    /** The column identifier for the {@link #value} field. */
    public static final String VALUE = "value";

    /** The qualified column identifier for the {@link #value} field. */
    public static final ColumnExp VALUE_C =
        new ColumnExp(RoomPropertyRecord.class, VALUE);
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
                new String[] { OWNER_ID, SCENE_ID, NAME },
                new Comparable[] { ownerId, sceneId, name });
    }
    // AUTO-GENERATED: METHODS END
}
