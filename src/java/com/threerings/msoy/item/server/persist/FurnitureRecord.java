//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.annotation.TableGenerator;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.item.data.all.Furniture;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MsoyItemType;

/**
 * Represents a piece of furniture (any prop really) that a user can place into a virtual world
 * scene and potentially interact with.
 */
@TableGenerator(name="itemId", pkColumnValue="FURNITURE")
public class FurnitureRecord extends ItemRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<FurnitureRecord> _R = FurnitureRecord.class;
    public static final ColumnExp ACTION = colexp(_R, "action");
    public static final ColumnExp HOT_SPOT_X = colexp(_R, "hotSpotX");
    public static final ColumnExp HOT_SPOT_Y = colexp(_R, "hotSpotY");
    public static final ColumnExp ITEM_ID = colexp(_R, "itemId");
    public static final ColumnExp SOURCE_ID = colexp(_R, "sourceId");
    public static final ColumnExp CREATOR_ID = colexp(_R, "creatorId");
    public static final ColumnExp OWNER_ID = colexp(_R, "ownerId");
    public static final ColumnExp CATALOG_ID = colexp(_R, "catalogId");
    public static final ColumnExp RATING_SUM = colexp(_R, "ratingSum");
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

    /** Update this version if you change fields specific to this derived class. */
    public static final int ITEM_VERSION = 2;

    /** This combines {@link #ITEM_VERSION} with {@link #BASE_SCHEMA_VERSION} to create a version
     * that allows us to make ItemRecord-wide changes and specific derived class changes. */
    public static final int SCHEMA_VERSION = ITEM_VERSION + BASE_SCHEMA_VERSION * BASE_MULTIPLIER;

    /** An action associated with this furniture which is dispatched to the virtual world client
     * when the furniture is clicked on (max length 255 characters). */
    public String action = "";

    /** The x position of the hot spot to use for this furniture. */
    public short hotSpotX;

    /** The y position of the hot spot to use for this furniture. */
    public short hotSpotY;

    @Override // from ItemRecord
    public MsoyItemType getType ()
    {
        return MsoyItemType.FURNITURE;
    }

    @Override // from ItemRecord
    public void fromItem (Item item)
    {
        super.fromItem(item);

        Furniture furniture = (Furniture)item;
        action = furniture.action;
        hotSpotX = furniture.hotSpotX;
        hotSpotY = furniture.hotSpotY;
    }

    @Override // from ItemRecord
    protected Item createItem ()
    {
        Furniture object = new Furniture();
        object.action = action;
        object.hotSpotX = hotSpotX;
        object.hotSpotY = hotSpotY;
        return object;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link FurnitureRecord}
     * with the supplied key values.
     */
    public static Key<FurnitureRecord> getKey (int itemId)
    {
        return newKey(_R, itemId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(ITEM_ID); }
    // AUTO-GENERATED: METHODS END
}
