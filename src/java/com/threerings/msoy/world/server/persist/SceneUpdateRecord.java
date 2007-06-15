//
// $Id$

package com.threerings.msoy.world.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.expression.ColumnExp;

/**
 * Contains serialized scene update data.
 */
public class SceneUpdateRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #sceneId} field. */
    public static final String SCENE_ID = "sceneId";

    /** The qualified column identifier for the {@link #sceneId} field. */
    public static final ColumnExp SCENE_ID_C =
        new ColumnExp(SceneUpdateRecord.class, SCENE_ID);

    /** The column identifier for the {@link #sceneVersion} field. */
    public static final String SCENE_VERSION = "sceneVersion";

    /** The qualified column identifier for the {@link #sceneVersion} field. */
    public static final ColumnExp SCENE_VERSION_C =
        new ColumnExp(SceneUpdateRecord.class, SCENE_VERSION);

    /** The column identifier for the {@link #updateType} field. */
    public static final String UPDATE_TYPE = "updateType";

    /** The qualified column identifier for the {@link #updateType} field. */
    public static final ColumnExp UPDATE_TYPE_C =
        new ColumnExp(SceneUpdateRecord.class, UPDATE_TYPE);

    /** The column identifier for the {@link #data} field. */
    public static final String DATA = "data";

    /** The qualified column identifier for the {@link #data} field. */
    public static final ColumnExp DATA_C =
        new ColumnExp(SceneUpdateRecord.class, DATA);
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** The id of the scene for which this update applies. */
    @Id public int sceneId;

    /** The version of the scene on which this update operates (the prior-to-being-updated
     * version. */
    @Id public int sceneVersion;

    /** The type of this update. */
    public int updateType;

    /** The serialized update data. */
    public byte[] data;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #SceneUpdateRecord}
     * with the supplied key values.
     */
    public static Key<SceneUpdateRecord> getKey (int sceneId, int sceneVersion)
    {
        return new Key<SceneUpdateRecord>(
                SceneUpdateRecord.class,
                new String[] { SCENE_ID, SCENE_VERSION },
                new Comparable[] { sceneId, sceneVersion });
    }
    // AUTO-GENERATED: METHODS END
}
