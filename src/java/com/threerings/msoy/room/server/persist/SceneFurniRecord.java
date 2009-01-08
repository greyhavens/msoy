//
// $Id$

package com.threerings.msoy.room.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.room.data.FurniData;
import com.threerings.msoy.room.data.MsoyLocation;

/**
 * Contains information about a piece of furni in a scene.
 */
@Entity
public class SceneFurniRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<SceneFurniRecord> _R = SceneFurniRecord.class;
    public static final ColumnExp SCENE_ID = colexp(_R, "sceneId");
    public static final ColumnExp FURNI_ID = colexp(_R, "furniId");
    public static final ColumnExp ITEM_TYPE = colexp(_R, "itemType");
    public static final ColumnExp ITEM_ID = colexp(_R, "itemId");
    public static final ColumnExp MEDIA_HASH = colexp(_R, "mediaHash");
    public static final ColumnExp MEDIA_TYPE = colexp(_R, "mediaType");
    public static final ColumnExp X = colexp(_R, "x");
    public static final ColumnExp Y = colexp(_R, "y");
    public static final ColumnExp Z = colexp(_R, "z");
    public static final ColumnExp LAYOUT_INFO = colexp(_R, "layoutInfo");
    public static final ColumnExp SCALE_X = colexp(_R, "scaleX");
    public static final ColumnExp SCALE_Y = colexp(_R, "scaleY");
    public static final ColumnExp ROTATION = colexp(_R, "rotation");
    public static final ColumnExp HOT_SPOT_X = colexp(_R, "hotSpotX");
    public static final ColumnExp HOT_SPOT_Y = colexp(_R, "hotSpotY");
    public static final ColumnExp ACTION_TYPE = colexp(_R, "actionType");
    public static final ColumnExp ACTION_DATA = colexp(_R, "actionData");
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
                new ColumnExp[] { SCENE_ID, FURNI_ID },
                new Comparable[] { sceneId, furniId });
    }
    // AUTO-GENERATED: METHODS END
}
