//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.annotation.TableGenerator;

import com.threerings.msoy.item.data.all.Pet;
import com.threerings.msoy.item.data.all.Item;

/**
 * Represents a pet that a user can place into a virtual world scene and potentially interact with.
 */
@TableGenerator(name="itemId", pkColumnValue="PET")
public class PetRecord extends ItemRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The qualified column identifier for the {@link #itemId} field. */
    public static final ColumnExp ITEM_ID_C =
        new ColumnExp(PetRecord.class, ITEM_ID);

    /** The qualified column identifier for the {@link #sourceId} field. */
    public static final ColumnExp SOURCE_ID_C =
        new ColumnExp(PetRecord.class, SOURCE_ID);

    /** The qualified column identifier for the {@link #creatorId} field. */
    public static final ColumnExp CREATOR_ID_C =
        new ColumnExp(PetRecord.class, CREATOR_ID);

    /** The qualified column identifier for the {@link #ownerId} field. */
    public static final ColumnExp OWNER_ID_C =
        new ColumnExp(PetRecord.class, OWNER_ID);

    /** The qualified column identifier for the {@link #catalogId} field. */
    public static final ColumnExp CATALOG_ID_C =
        new ColumnExp(PetRecord.class, CATALOG_ID);

    /** The qualified column identifier for the {@link #rating} field. */
    public static final ColumnExp RATING_C =
        new ColumnExp(PetRecord.class, RATING);

    /** The qualified column identifier for the {@link #ratingCount} field. */
    public static final ColumnExp RATING_COUNT_C =
        new ColumnExp(PetRecord.class, RATING_COUNT);

    /** The qualified column identifier for the {@link #used} field. */
    public static final ColumnExp USED_C =
        new ColumnExp(PetRecord.class, USED);

    /** The qualified column identifier for the {@link #location} field. */
    public static final ColumnExp LOCATION_C =
        new ColumnExp(PetRecord.class, LOCATION);

    /** The qualified column identifier for the {@link #lastTouched} field. */
    public static final ColumnExp LAST_TOUCHED_C =
        new ColumnExp(PetRecord.class, LAST_TOUCHED);

    /** The qualified column identifier for the {@link #name} field. */
    public static final ColumnExp NAME_C =
        new ColumnExp(PetRecord.class, NAME);

    /** The qualified column identifier for the {@link #description} field. */
    public static final ColumnExp DESCRIPTION_C =
        new ColumnExp(PetRecord.class, DESCRIPTION);

    /** The qualified column identifier for the {@link #mature} field. */
    public static final ColumnExp MATURE_C =
        new ColumnExp(PetRecord.class, MATURE);

    /** The qualified column identifier for the {@link #thumbMediaHash} field. */
    public static final ColumnExp THUMB_MEDIA_HASH_C =
        new ColumnExp(PetRecord.class, THUMB_MEDIA_HASH);

    /** The qualified column identifier for the {@link #thumbMimeType} field. */
    public static final ColumnExp THUMB_MIME_TYPE_C =
        new ColumnExp(PetRecord.class, THUMB_MIME_TYPE);

    /** The qualified column identifier for the {@link #thumbConstraint} field. */
    public static final ColumnExp THUMB_CONSTRAINT_C =
        new ColumnExp(PetRecord.class, THUMB_CONSTRAINT);

    /** The qualified column identifier for the {@link #furniMediaHash} field. */
    public static final ColumnExp FURNI_MEDIA_HASH_C =
        new ColumnExp(PetRecord.class, FURNI_MEDIA_HASH);

    /** The qualified column identifier for the {@link #furniMimeType} field. */
    public static final ColumnExp FURNI_MIME_TYPE_C =
        new ColumnExp(PetRecord.class, FURNI_MIME_TYPE);

    /** The qualified column identifier for the {@link #furniConstraint} field. */
    public static final ColumnExp FURNI_CONSTRAINT_C =
        new ColumnExp(PetRecord.class, FURNI_CONSTRAINT);
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1 +
        BASE_SCHEMA_VERSION * BASE_MULTIPLIER;

    @Override // from ItemRecord
    public byte getType ()
    {
        return Item.PET;
    }

    @Override // from ItemRecord
    public void fromItem (Item item)
    {
        super.fromItem(item);

        // Pet pet = (Pet)item;
        // nothing custom to extract
    }

    @Override // from ItemRecord
    protected Item createItem ()
    {
        Pet object = new Pet();
        // nothing custom to set
        return object;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link PetRecord}
     * with the supplied key values.
     */
    public static Key<PetRecord> getKey (int itemId)
    {
        return new Key<PetRecord>(
                PetRecord.class,
                new String[] { ITEM_ID },
                new Comparable[] { itemId });
    }
    // AUTO-GENERATED: METHODS END
}
