//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.annotation.TableGenerator;

import com.threerings.msoy.item.data.all.Toy;
import com.threerings.msoy.item.data.all.Item;

/**
 * Represents a piece of furniture (any prop really) that a user can place into
 * a virtual world scene and potentially interact with.
 */
@TableGenerator(name="itemId", pkColumnValue="FURNITURE")
public class ToyRecord extends ItemRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The qualified column identifier for the {@link #itemId} field. */
    public static final ColumnExp ITEM_ID_C =
        new ColumnExp(ToyRecord.class, ITEM_ID);

    /** The qualified column identifier for the {@link #sourceId} field. */
    public static final ColumnExp SOURCE_ID_C =
        new ColumnExp(ToyRecord.class, SOURCE_ID);

    /** The qualified column identifier for the {@link #creatorId} field. */
    public static final ColumnExp CREATOR_ID_C =
        new ColumnExp(ToyRecord.class, CREATOR_ID);

    /** The qualified column identifier for the {@link #ownerId} field. */
    public static final ColumnExp OWNER_ID_C =
        new ColumnExp(ToyRecord.class, OWNER_ID);

    /** The qualified column identifier for the {@link #catalogId} field. */
    public static final ColumnExp CATALOG_ID_C =
        new ColumnExp(ToyRecord.class, CATALOG_ID);

    /** The qualified column identifier for the {@link #rating} field. */
    public static final ColumnExp RATING_C =
        new ColumnExp(ToyRecord.class, RATING);

    /** The qualified column identifier for the {@link #ratingCount} field. */
    public static final ColumnExp RATING_COUNT_C =
        new ColumnExp(ToyRecord.class, RATING_COUNT);

    /** The qualified column identifier for the {@link #used} field. */
    public static final ColumnExp USED_C =
        new ColumnExp(ToyRecord.class, USED);

    /** The qualified column identifier for the {@link #location} field. */
    public static final ColumnExp LOCATION_C =
        new ColumnExp(ToyRecord.class, LOCATION);

    /** The qualified column identifier for the {@link #lastTouched} field. */
    public static final ColumnExp LAST_TOUCHED_C =
        new ColumnExp(ToyRecord.class, LAST_TOUCHED);

    /** The qualified column identifier for the {@link #name} field. */
    public static final ColumnExp NAME_C =
        new ColumnExp(ToyRecord.class, NAME);

    /** The qualified column identifier for the {@link #description} field. */
    public static final ColumnExp DESCRIPTION_C =
        new ColumnExp(ToyRecord.class, DESCRIPTION);

    /** The qualified column identifier for the {@link #mature} field. */
    public static final ColumnExp MATURE_C =
        new ColumnExp(ToyRecord.class, MATURE);

    /** The qualified column identifier for the {@link #thumbMediaHash} field. */
    public static final ColumnExp THUMB_MEDIA_HASH_C =
        new ColumnExp(ToyRecord.class, THUMB_MEDIA_HASH);

    /** The qualified column identifier for the {@link #thumbMimeType} field. */
    public static final ColumnExp THUMB_MIME_TYPE_C =
        new ColumnExp(ToyRecord.class, THUMB_MIME_TYPE);

    /** The qualified column identifier for the {@link #thumbConstraint} field. */
    public static final ColumnExp THUMB_CONSTRAINT_C =
        new ColumnExp(ToyRecord.class, THUMB_CONSTRAINT);

    /** The qualified column identifier for the {@link #furniMediaHash} field. */
    public static final ColumnExp FURNI_MEDIA_HASH_C =
        new ColumnExp(ToyRecord.class, FURNI_MEDIA_HASH);

    /** The qualified column identifier for the {@link #furniMimeType} field. */
    public static final ColumnExp FURNI_MIME_TYPE_C =
        new ColumnExp(ToyRecord.class, FURNI_MIME_TYPE);

    /** The qualified column identifier for the {@link #furniConstraint} field. */
    public static final ColumnExp FURNI_CONSTRAINT_C =
        new ColumnExp(ToyRecord.class, FURNI_CONSTRAINT);
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1 +
        BASE_SCHEMA_VERSION * BASE_MULTIPLIER;

    @Override // from ItemRecord
    public byte getType ()
    {
        return Item.TOY;
    }

    @Override // from ItemRecord
    public void fromItem (Item item)
    {
        super.fromItem(item);

        // Toy toy = (Toy)item;
        // nothing for now
    }

    @Override // from ItemRecord
    protected Item createItem ()
    {
        Toy object = new Toy();
        // nothing for now
        return object;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link ToyRecord}
     * with the supplied key values.
     */
    public static Key<ToyRecord> getKey (int itemId)
    {
        return new Key<ToyRecord>(
                ToyRecord.class,
                new String[] { ITEM_ID },
                new Comparable[] { itemId });
    }
    // AUTO-GENERATED: METHODS END
}
