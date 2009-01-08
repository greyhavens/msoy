//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.TableGenerator;
import com.samskivert.depot.expression.ColumnExp;

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
    public static final Class<DecorRecord> _R = DecorRecord.class;
    public static final ColumnExp TYPE = colexp(_R, "type");
    public static final ColumnExp HEIGHT = colexp(_R, "height");
    public static final ColumnExp WIDTH = colexp(_R, "width");
    public static final ColumnExp DEPTH = colexp(_R, "depth");
    public static final ColumnExp HORIZON = colexp(_R, "horizon");
    public static final ColumnExp HIDE_WALLS = colexp(_R, "hideWalls");
    public static final ColumnExp ACTOR_SCALE = colexp(_R, "actorScale");
    public static final ColumnExp FURNI_SCALE = colexp(_R, "furniScale");
    public static final ColumnExp ITEM_ID = colexp(_R, "itemId");
    public static final ColumnExp SOURCE_ID = colexp(_R, "sourceId");
    public static final ColumnExp CREATOR_ID = colexp(_R, "creatorId");
    public static final ColumnExp OWNER_ID = colexp(_R, "ownerId");
    public static final ColumnExp CATALOG_ID = colexp(_R, "catalogId");
    public static final ColumnExp RATING = colexp(_R, "rating");
    public static final ColumnExp RATING_COUNT = colexp(_R, "ratingCount");
    public static final ColumnExp USED = colexp(_R, "used");
    public static final ColumnExp LOCATION = colexp(_R, "location");
    public static final ColumnExp LAST_TOUCHED = colexp(_R, "lastTouched");
    public static final ColumnExp NAME = colexp(_R, "name");
    public static final ColumnExp DESCRIPTION = colexp(_R, "description");
    public static final ColumnExp MATURE = colexp(_R, "mature");
    public static final ColumnExp THUMB_MEDIA_HASH = colexp(_R, "thumbMediaHash");
    public static final ColumnExp THUMB_MIME_TYPE = colexp(_R, "thumbMimeType");
    public static final ColumnExp THUMB_CONSTRAINT = colexp(_R, "thumbConstraint");
    public static final ColumnExp FURNI_MEDIA_HASH = colexp(_R, "furniMediaHash");
    public static final ColumnExp FURNI_MIME_TYPE = colexp(_R, "furniMimeType");
    public static final ColumnExp FURNI_CONSTRAINT = colexp(_R, "furniConstraint");
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
                new ColumnExp[] { ITEM_ID },
                new Comparable[] { itemId });
    }
    // AUTO-GENERATED: METHODS END
}
