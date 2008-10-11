//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.annotation.Column;
import com.samskivert.jdbc.depot.annotation.TableGenerator;
import com.samskivert.jdbc.depot.expression.ColumnExp;

import com.threerings.msoy.item.data.all.Decor;
import com.threerings.msoy.item.data.all.Item;

/**
 * Represents a piece of decor (any prop really) that a user can place into
 * a virtual world scene and potentially interact with.
 */
@TableGenerator(name="itemId", pkColumnValue="DECOR")
public class DecorRecord extends ItemRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #type} field. */
    public static final String TYPE = "type";

    /** The qualified column identifier for the {@link #type} field. */
    public static final ColumnExp TYPE_C =
        new ColumnExp(DecorRecord.class, TYPE);

    /** The column identifier for the {@link #height} field. */
    public static final String HEIGHT = "height";

    /** The qualified column identifier for the {@link #height} field. */
    public static final ColumnExp HEIGHT_C =
        new ColumnExp(DecorRecord.class, HEIGHT);

    /** The column identifier for the {@link #width} field. */
    public static final String WIDTH = "width";

    /** The qualified column identifier for the {@link #width} field. */
    public static final ColumnExp WIDTH_C =
        new ColumnExp(DecorRecord.class, WIDTH);

    /** The column identifier for the {@link #depth} field. */
    public static final String DEPTH = "depth";

    /** The qualified column identifier for the {@link #depth} field. */
    public static final ColumnExp DEPTH_C =
        new ColumnExp(DecorRecord.class, DEPTH);

    /** The column identifier for the {@link #horizon} field. */
    public static final String HORIZON = "horizon";

    /** The qualified column identifier for the {@link #horizon} field. */
    public static final ColumnExp HORIZON_C =
        new ColumnExp(DecorRecord.class, HORIZON);

    /** The column identifier for the {@link #hideWalls} field. */
    public static final String HIDE_WALLS = "hideWalls";

    /** The qualified column identifier for the {@link #hideWalls} field. */
    public static final ColumnExp HIDE_WALLS_C =
        new ColumnExp(DecorRecord.class, HIDE_WALLS);

    /** The column identifier for the {@link #actorScale} field. */
    public static final String ACTOR_SCALE = "actorScale";

    /** The qualified column identifier for the {@link #actorScale} field. */
    public static final ColumnExp ACTOR_SCALE_C =
        new ColumnExp(DecorRecord.class, ACTOR_SCALE);

    /** The column identifier for the {@link #furniScale} field. */
    public static final String FURNI_SCALE = "furniScale";

    /** The qualified column identifier for the {@link #furniScale} field. */
    public static final ColumnExp FURNI_SCALE_C =
        new ColumnExp(DecorRecord.class, FURNI_SCALE);

    /** The qualified column identifier for the {@link #itemId} field. */
    public static final ColumnExp ITEM_ID_C =
        new ColumnExp(DecorRecord.class, ITEM_ID);

    /** The qualified column identifier for the {@link #sourceId} field. */
    public static final ColumnExp SOURCE_ID_C =
        new ColumnExp(DecorRecord.class, SOURCE_ID);

    /** The qualified column identifier for the {@link #flagged} field. */
    public static final ColumnExp FLAGGED_C =
        new ColumnExp(DecorRecord.class, FLAGGED);

    /** The qualified column identifier for the {@link #creatorId} field. */
    public static final ColumnExp CREATOR_ID_C =
        new ColumnExp(DecorRecord.class, CREATOR_ID);

    /** The qualified column identifier for the {@link #ownerId} field. */
    public static final ColumnExp OWNER_ID_C =
        new ColumnExp(DecorRecord.class, OWNER_ID);

    /** The qualified column identifier for the {@link #catalogId} field. */
    public static final ColumnExp CATALOG_ID_C =
        new ColumnExp(DecorRecord.class, CATALOG_ID);

    /** The qualified column identifier for the {@link #rating} field. */
    public static final ColumnExp RATING_C =
        new ColumnExp(DecorRecord.class, RATING);

    /** The qualified column identifier for the {@link #ratingCount} field. */
    public static final ColumnExp RATING_COUNT_C =
        new ColumnExp(DecorRecord.class, RATING_COUNT);

    /** The qualified column identifier for the {@link #used} field. */
    public static final ColumnExp USED_C =
        new ColumnExp(DecorRecord.class, USED);

    /** The qualified column identifier for the {@link #location} field. */
    public static final ColumnExp LOCATION_C =
        new ColumnExp(DecorRecord.class, LOCATION);

    /** The qualified column identifier for the {@link #lastTouched} field. */
    public static final ColumnExp LAST_TOUCHED_C =
        new ColumnExp(DecorRecord.class, LAST_TOUCHED);

    /** The qualified column identifier for the {@link #name} field. */
    public static final ColumnExp NAME_C =
        new ColumnExp(DecorRecord.class, NAME);

    /** The qualified column identifier for the {@link #description} field. */
    public static final ColumnExp DESCRIPTION_C =
        new ColumnExp(DecorRecord.class, DESCRIPTION);

    /** The qualified column identifier for the {@link #mature} field. */
    public static final ColumnExp MATURE_C =
        new ColumnExp(DecorRecord.class, MATURE);

    /** The qualified column identifier for the {@link #thumbMediaHash} field. */
    public static final ColumnExp THUMB_MEDIA_HASH_C =
        new ColumnExp(DecorRecord.class, THUMB_MEDIA_HASH);

    /** The qualified column identifier for the {@link #thumbMimeType} field. */
    public static final ColumnExp THUMB_MIME_TYPE_C =
        new ColumnExp(DecorRecord.class, THUMB_MIME_TYPE);

    /** The qualified column identifier for the {@link #thumbConstraint} field. */
    public static final ColumnExp THUMB_CONSTRAINT_C =
        new ColumnExp(DecorRecord.class, THUMB_CONSTRAINT);

    /** The qualified column identifier for the {@link #furniMediaHash} field. */
    public static final ColumnExp FURNI_MEDIA_HASH_C =
        new ColumnExp(DecorRecord.class, FURNI_MEDIA_HASH);

    /** The qualified column identifier for the {@link #furniMimeType} field. */
    public static final ColumnExp FURNI_MIME_TYPE_C =
        new ColumnExp(DecorRecord.class, FURNI_MIME_TYPE);

    /** The qualified column identifier for the {@link #furniConstraint} field. */
    public static final ColumnExp FURNI_CONSTRAINT_C =
        new ColumnExp(DecorRecord.class, FURNI_CONSTRAINT);
    // AUTO-GENERATED: FIELDS END

    /** Room type. Controls how the background wallpaper image is handled. */
    public byte type;

    /** Room height, in pixels. */
    public short height;

    /** Room width, in pixels. */
    public short width;

    /** Room depth, in pixels. */
    public short depth;

    /** Horizon position, in [0, 1]. */
    public float horizon;

    /** Specifies whether side walls should be displayed. */
    public boolean hideWalls;

    /** The adjusted scale of actors in this room. */
    @Column(defaultValue="1")
    public float actorScale;

    /** The adjusted scale of furni in this room. */
    @Column(defaultValue="1")
    public float furniScale;

    public static final int SCHEMA_VERSION = 4 +
        BASE_SCHEMA_VERSION * BASE_MULTIPLIER;

    @Override // from ItemRecord
    public byte getType ()
    {
        return Item.DECOR;
    }

    @Override // from ItemRecord
    public void fromItem (Item item)
    {
        super.fromItem(item);

        Decor decor = (Decor)item;
        type = decor.type;
        height = decor.height;
        width = decor.width;
        depth = decor.depth;
        horizon = decor.horizon;
        hideWalls = decor.hideWalls;
        actorScale = decor.actorScale;
        furniScale = decor.furniScale;
    }

    @Override // from ItemRecord
    protected Item createItem ()
    {
        Decor object = new Decor();
        object.type = type;
        object.height = height;
        object.width = width;
        object.depth = depth;
        object.horizon = horizon;
        object.hideWalls = hideWalls;
        object.actorScale = actorScale;
        object.furniScale = furniScale;
        return object;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link DecorRecord}
     * with the supplied key values.
     */
    public static Key<DecorRecord> getKey (int itemId)
    {
        return new Key<DecorRecord>(
                DecorRecord.class,
                new String[] { ITEM_ID },
                new Comparable[] { itemId });
    }
    // AUTO-GENERATED: METHODS END
}
