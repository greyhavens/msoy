//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.annotation.TableGenerator;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.item.data.all.Furniture;
import com.threerings.msoy.item.data.all.Item;

/**
 * Represents a piece of furniture (any prop really) that a user can place into a virtual world
 * scene and potentially interact with.
 */
@TableGenerator(name="itemId", pkColumnValue="FURNITURE")
public class FurnitureRecord extends ItemRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #action} field. */
    public static final String ACTION = "action";

    /** The qualified column identifier for the {@link #action} field. */
    public static final ColumnExp ACTION_C =
        new ColumnExp(FurnitureRecord.class, ACTION);

    /** The column identifier for the {@link #hotSpotX} field. */
    public static final String HOT_SPOT_X = "hotSpotX";

    /** The qualified column identifier for the {@link #hotSpotX} field. */
    public static final ColumnExp HOT_SPOT_X_C =
        new ColumnExp(FurnitureRecord.class, HOT_SPOT_X);

    /** The column identifier for the {@link #hotSpotY} field. */
    public static final String HOT_SPOT_Y = "hotSpotY";

    /** The qualified column identifier for the {@link #hotSpotY} field. */
    public static final ColumnExp HOT_SPOT_Y_C =
        new ColumnExp(FurnitureRecord.class, HOT_SPOT_Y);

    /** The qualified column identifier for the {@link #itemId} field. */
    public static final ColumnExp ITEM_ID_C =
        new ColumnExp(FurnitureRecord.class, ITEM_ID);

    /** The qualified column identifier for the {@link #sourceId} field. */
    public static final ColumnExp SOURCE_ID_C =
        new ColumnExp(FurnitureRecord.class, SOURCE_ID);

    /** The qualified column identifier for the {@link #flagged} field. */
    public static final ColumnExp FLAGGED_C =
        new ColumnExp(FurnitureRecord.class, FLAGGED);

    /** The qualified column identifier for the {@link #creatorId} field. */
    public static final ColumnExp CREATOR_ID_C =
        new ColumnExp(FurnitureRecord.class, CREATOR_ID);

    /** The qualified column identifier for the {@link #ownerId} field. */
    public static final ColumnExp OWNER_ID_C =
        new ColumnExp(FurnitureRecord.class, OWNER_ID);

    /** The qualified column identifier for the {@link #catalogId} field. */
    public static final ColumnExp CATALOG_ID_C =
        new ColumnExp(FurnitureRecord.class, CATALOG_ID);

    /** The qualified column identifier for the {@link #rating} field. */
    public static final ColumnExp RATING_C =
        new ColumnExp(FurnitureRecord.class, RATING);

    /** The qualified column identifier for the {@link #ratingCount} field. */
    public static final ColumnExp RATING_COUNT_C =
        new ColumnExp(FurnitureRecord.class, RATING_COUNT);

    /** The qualified column identifier for the {@link #used} field. */
    public static final ColumnExp USED_C =
        new ColumnExp(FurnitureRecord.class, USED);

    /** The qualified column identifier for the {@link #location} field. */
    public static final ColumnExp LOCATION_C =
        new ColumnExp(FurnitureRecord.class, LOCATION);

    /** The qualified column identifier for the {@link #lastTouched} field. */
    public static final ColumnExp LAST_TOUCHED_C =
        new ColumnExp(FurnitureRecord.class, LAST_TOUCHED);

    /** The qualified column identifier for the {@link #name} field. */
    public static final ColumnExp NAME_C =
        new ColumnExp(FurnitureRecord.class, NAME);

    /** The qualified column identifier for the {@link #description} field. */
    public static final ColumnExp DESCRIPTION_C =
        new ColumnExp(FurnitureRecord.class, DESCRIPTION);

    /** The qualified column identifier for the {@link #mature} field. */
    public static final ColumnExp MATURE_C =
        new ColumnExp(FurnitureRecord.class, MATURE);

    /** The qualified column identifier for the {@link #thumbMediaHash} field. */
    public static final ColumnExp THUMB_MEDIA_HASH_C =
        new ColumnExp(FurnitureRecord.class, THUMB_MEDIA_HASH);

    /** The qualified column identifier for the {@link #thumbMimeType} field. */
    public static final ColumnExp THUMB_MIME_TYPE_C =
        new ColumnExp(FurnitureRecord.class, THUMB_MIME_TYPE);

    /** The qualified column identifier for the {@link #thumbConstraint} field. */
    public static final ColumnExp THUMB_CONSTRAINT_C =
        new ColumnExp(FurnitureRecord.class, THUMB_CONSTRAINT);

    /** The qualified column identifier for the {@link #furniMediaHash} field. */
    public static final ColumnExp FURNI_MEDIA_HASH_C =
        new ColumnExp(FurnitureRecord.class, FURNI_MEDIA_HASH);

    /** The qualified column identifier for the {@link #furniMimeType} field. */
    public static final ColumnExp FURNI_MIME_TYPE_C =
        new ColumnExp(FurnitureRecord.class, FURNI_MIME_TYPE);

    /** The qualified column identifier for the {@link #furniConstraint} field. */
    public static final ColumnExp FURNI_CONSTRAINT_C =
        new ColumnExp(FurnitureRecord.class, FURNI_CONSTRAINT);
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 2 + BASE_SCHEMA_VERSION * BASE_MULTIPLIER;

    /** An action associated with this furniture which is dispatched to the virtual world client
     * when the furniture is clicked on (max length 255 characters). */
    public String action = "";

    /** The x position of the hot spot to use for this furniture. */
    public short hotSpotX;

    /** The y position of the hot spot to use for this furniture. */
    public short hotSpotY;

    @Override // from ItemRecord
    public byte getType ()
    {
        return Item.FURNITURE;
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
        return new Key<FurnitureRecord>(
                FurnitureRecord.class,
                new String[] { ITEM_ID },
                new Comparable[] { itemId });
    }
    // AUTO-GENERATED: METHODS END
}
