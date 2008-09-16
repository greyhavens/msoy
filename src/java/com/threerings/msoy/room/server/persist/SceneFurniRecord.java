//
// $Id$

package com.threerings.msoy.room.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Column;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.expression.ColumnExp;

import com.threerings.msoy.room.data.FurniData;
import com.threerings.msoy.room.data.MsoyLocation;

/**
 * Contains information about a piece of furni in a scene.
 */
@Entity
public class SceneFurniRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #sceneId} field. */
    public static final String SCENE_ID = "sceneId";

    /** The qualified column identifier for the {@link #sceneId} field. */
    public static final ColumnExp SCENE_ID_C =
        new ColumnExp(SceneFurniRecord.class, SCENE_ID);

    /** The column identifier for the {@link #furniId} field. */
    public static final String FURNI_ID = "furniId";

    /** The qualified column identifier for the {@link #furniId} field. */
    public static final ColumnExp FURNI_ID_C =
        new ColumnExp(SceneFurniRecord.class, FURNI_ID);

    /** The column identifier for the {@link #itemType} field. */
    public static final String ITEM_TYPE = "itemType";

    /** The qualified column identifier for the {@link #itemType} field. */
    public static final ColumnExp ITEM_TYPE_C =
        new ColumnExp(SceneFurniRecord.class, ITEM_TYPE);

    /** The column identifier for the {@link #itemId} field. */
    public static final String ITEM_ID = "itemId";

    /** The qualified column identifier for the {@link #itemId} field. */
    public static final ColumnExp ITEM_ID_C =
        new ColumnExp(SceneFurniRecord.class, ITEM_ID);

    /** The column identifier for the {@link #mediaHash} field. */
    public static final String MEDIA_HASH = "mediaHash";

    /** The qualified column identifier for the {@link #mediaHash} field. */
    public static final ColumnExp MEDIA_HASH_C =
        new ColumnExp(SceneFurniRecord.class, MEDIA_HASH);

    /** The column identifier for the {@link #mediaType} field. */
    public static final String MEDIA_TYPE = "mediaType";

    /** The qualified column identifier for the {@link #mediaType} field. */
    public static final ColumnExp MEDIA_TYPE_C =
        new ColumnExp(SceneFurniRecord.class, MEDIA_TYPE);

    /** The column identifier for the {@link #x} field. */
    public static final String X = "x";

    /** The qualified column identifier for the {@link #x} field. */
    public static final ColumnExp X_C =
        new ColumnExp(SceneFurniRecord.class, X);

    /** The column identifier for the {@link #y} field. */
    public static final String Y = "y";

    /** The qualified column identifier for the {@link #y} field. */
    public static final ColumnExp Y_C =
        new ColumnExp(SceneFurniRecord.class, Y);

    /** The column identifier for the {@link #z} field. */
    public static final String Z = "z";

    /** The qualified column identifier for the {@link #z} field. */
    public static final ColumnExp Z_C =
        new ColumnExp(SceneFurniRecord.class, Z);

    /** The column identifier for the {@link #layoutInfo} field. */
    public static final String LAYOUT_INFO = "layoutInfo";

    /** The qualified column identifier for the {@link #layoutInfo} field. */
    public static final ColumnExp LAYOUT_INFO_C =
        new ColumnExp(SceneFurniRecord.class, LAYOUT_INFO);

    /** The column identifier for the {@link #scaleX} field. */
    public static final String SCALE_X = "scaleX";

    /** The qualified column identifier for the {@link #scaleX} field. */
    public static final ColumnExp SCALE_X_C =
        new ColumnExp(SceneFurniRecord.class, SCALE_X);

    /** The column identifier for the {@link #scaleY} field. */
    public static final String SCALE_Y = "scaleY";

    /** The qualified column identifier for the {@link #scaleY} field. */
    public static final ColumnExp SCALE_Y_C =
        new ColumnExp(SceneFurniRecord.class, SCALE_Y);

    /** The column identifier for the {@link #rotation} field. */
    public static final String ROTATION = "rotation";

    /** The qualified column identifier for the {@link #rotation} field. */
    public static final ColumnExp ROTATION_C =
        new ColumnExp(SceneFurniRecord.class, ROTATION);

    /** The column identifier for the {@link #hotSpotX} field. */
    public static final String HOT_SPOT_X = "hotSpotX";

    /** The qualified column identifier for the {@link #hotSpotX} field. */
    public static final ColumnExp HOT_SPOT_X_C =
        new ColumnExp(SceneFurniRecord.class, HOT_SPOT_X);

    /** The column identifier for the {@link #hotSpotY} field. */
    public static final String HOT_SPOT_Y = "hotSpotY";

    /** The qualified column identifier for the {@link #hotSpotY} field. */
    public static final ColumnExp HOT_SPOT_Y_C =
        new ColumnExp(SceneFurniRecord.class, HOT_SPOT_Y);

    /** The column identifier for the {@link #actionType} field. */
    public static final String ACTION_TYPE = "actionType";

    /** The qualified column identifier for the {@link #actionType} field. */
    public static final ColumnExp ACTION_TYPE_C =
        new ColumnExp(SceneFurniRecord.class, ACTION_TYPE);

    /** The column identifier for the {@link #actionData} field. */
    public static final String ACTION_DATA = "actionData";

    /** The qualified column identifier for the {@link #actionData} field. */
    public static final ColumnExp ACTION_DATA_C =
        new ColumnExp(SceneFurniRecord.class, ACTION_DATA);
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 3;

    /** The id of the scene in which this furni is placed. */
    @Id public int sceneId;

    /** An identifier assigned to this furni data, unique only to the containing scene. */
    @Id public short furniId;

    /** The type of item being used as furni. */
    public byte itemType;

    /** The identifier of the item being used as furni. */
    public int itemId;

    /** The item's furni media hash. */
    public byte[] mediaHash;

    /** The item's furni media mime type. */
    public byte mediaType;

    /** The furni's logical X position in the scene. */
    public float x;

    /** The furni's logical Y position in the scene. */
    public float y;

    /** The furni's logical Z position in the scene. */
    public float z;

    /** TODO: Layout info? */
    public byte layoutInfo;

    /** The furni's X scale. */
    public float scaleX;

    /** The furni's Y scale. */
    public float scaleY;

    /** Rotation angle in degrees. */
    public float rotation;

    /** The x location of this furniture's hot spot. */
    public short hotSpotX;

    /** The y location of this furniture's hot spot. */
    public short hotSpotY;

    /** The action to be taken when this furni is clicked. */
    public byte actionType;

    /** Any argument to this furni's action. */
    @Column(nullable=true)
    public String actionData;

    /** Used when loading from the database. */
    public SceneFurniRecord ()
    {
    }

    /**
     * Creates and populates a persistent record from the supplied runtime record.
     */
    public SceneFurniRecord (int sceneId, FurniData data)
    {
        this.sceneId = sceneId;
        furniId = data.id;
        itemType = data.itemType;
        itemId = data.itemId;
        mediaHash = SceneUtil.flattenMediaDesc(data.media);
        mediaType = data.media.mimeType;
        x = data.loc.x;
        y = data.loc.y;
        z = data.loc.z;
        layoutInfo = data.layoutInfo;
        scaleX = data.scaleX;
        scaleY = data.scaleY;
        rotation = data.rotation;
        hotSpotX = data.hotSpotX;
        hotSpotY = data.hotSpotY;
        actionType = data.actionType;
        actionData = data.actionData;
    }

    /**
     * Converts this persistent record to the corresponding runtime record.
     */
    public FurniData toFurniData ()
    {
        FurniData furni = new FurniData();
        furni.id = furniId;
        furni.itemType = itemType;
        furni.itemId = itemId;
        furni.media = SceneUtil.createMediaDesc(mediaHash, mediaType);
        furni.loc = new MsoyLocation(x, y, z, 0);
        furni.layoutInfo = layoutInfo;
        furni.scaleX = scaleX;
        furni.scaleY = scaleY;
        furni.rotation = rotation;
        furni.hotSpotX = hotSpotX;
        furni.hotSpotY = hotSpotY;
        furni.actionType = actionType;
        furni.actionData = actionData;
        return furni;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link SceneFurniRecord}
     * with the supplied key values.
     */
    public static Key<SceneFurniRecord> getKey (int sceneId, short furniId)
    {
        return new Key<SceneFurniRecord>(
                SceneFurniRecord.class,
                new String[] { SCENE_ID, FURNI_ID },
                new Comparable[] { sceneId, furniId });
    }
    // AUTO-GENERATED: METHODS END
}
