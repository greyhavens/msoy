//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.annotation.TableGenerator;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.Prize;

/**
 * Contains the persistent data for a Prize item.
 */
@TableGenerator(name="itemId", pkColumnValue="PRIZE")
public class PrizeRecord extends SubItemRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #targetType} field. */
    public static final String TARGET_TYPE = "targetType";

    /** The qualified column identifier for the {@link #targetType} field. */
    public static final ColumnExp TARGET_TYPE_C =
        new ColumnExp(PrizeRecord.class, TARGET_TYPE);

    /** The column identifier for the {@link #targetCatalogId} field. */
    public static final String TARGET_CATALOG_ID = "targetCatalogId";

    /** The qualified column identifier for the {@link #targetCatalogId} field. */
    public static final ColumnExp TARGET_CATALOG_ID_C =
        new ColumnExp(PrizeRecord.class, TARGET_CATALOG_ID);

    /** The qualified column identifier for the {@link #suiteId} field. */
    public static final ColumnExp SUITE_ID_C =
        new ColumnExp(PrizeRecord.class, SUITE_ID);

    /** The qualified column identifier for the {@link #ident} field. */
    public static final ColumnExp IDENT_C =
        new ColumnExp(PrizeRecord.class, IDENT);

    /** The qualified column identifier for the {@link #itemId} field. */
    public static final ColumnExp ITEM_ID_C =
        new ColumnExp(PrizeRecord.class, ITEM_ID);

    /** The qualified column identifier for the {@link #sourceId} field. */
    public static final ColumnExp SOURCE_ID_C =
        new ColumnExp(PrizeRecord.class, SOURCE_ID);

    /** The qualified column identifier for the {@link #creatorId} field. */
    public static final ColumnExp CREATOR_ID_C =
        new ColumnExp(PrizeRecord.class, CREATOR_ID);

    /** The qualified column identifier for the {@link #ownerId} field. */
    public static final ColumnExp OWNER_ID_C =
        new ColumnExp(PrizeRecord.class, OWNER_ID);

    /** The qualified column identifier for the {@link #catalogId} field. */
    public static final ColumnExp CATALOG_ID_C =
        new ColumnExp(PrizeRecord.class, CATALOG_ID);

    /** The qualified column identifier for the {@link #rating} field. */
    public static final ColumnExp RATING_C =
        new ColumnExp(PrizeRecord.class, RATING);

    /** The qualified column identifier for the {@link #ratingCount} field. */
    public static final ColumnExp RATING_COUNT_C =
        new ColumnExp(PrizeRecord.class, RATING_COUNT);

    /** The qualified column identifier for the {@link #used} field. */
    public static final ColumnExp USED_C =
        new ColumnExp(PrizeRecord.class, USED);

    /** The qualified column identifier for the {@link #location} field. */
    public static final ColumnExp LOCATION_C =
        new ColumnExp(PrizeRecord.class, LOCATION);

    /** The qualified column identifier for the {@link #lastTouched} field. */
    public static final ColumnExp LAST_TOUCHED_C =
        new ColumnExp(PrizeRecord.class, LAST_TOUCHED);

    /** The qualified column identifier for the {@link #name} field. */
    public static final ColumnExp NAME_C =
        new ColumnExp(PrizeRecord.class, NAME);

    /** The qualified column identifier for the {@link #description} field. */
    public static final ColumnExp DESCRIPTION_C =
        new ColumnExp(PrizeRecord.class, DESCRIPTION);

    /** The qualified column identifier for the {@link #mature} field. */
    public static final ColumnExp MATURE_C =
        new ColumnExp(PrizeRecord.class, MATURE);

    /** The qualified column identifier for the {@link #thumbMediaHash} field. */
    public static final ColumnExp THUMB_MEDIA_HASH_C =
        new ColumnExp(PrizeRecord.class, THUMB_MEDIA_HASH);

    /** The qualified column identifier for the {@link #thumbMimeType} field. */
    public static final ColumnExp THUMB_MIME_TYPE_C =
        new ColumnExp(PrizeRecord.class, THUMB_MIME_TYPE);

    /** The qualified column identifier for the {@link #thumbConstraint} field. */
    public static final ColumnExp THUMB_CONSTRAINT_C =
        new ColumnExp(PrizeRecord.class, THUMB_CONSTRAINT);

    /** The qualified column identifier for the {@link #furniMediaHash} field. */
    public static final ColumnExp FURNI_MEDIA_HASH_C =
        new ColumnExp(PrizeRecord.class, FURNI_MEDIA_HASH);

    /** The qualified column identifier for the {@link #furniMimeType} field. */
    public static final ColumnExp FURNI_MIME_TYPE_C =
        new ColumnExp(PrizeRecord.class, FURNI_MIME_TYPE);

    /** The qualified column identifier for the {@link #furniConstraint} field. */
    public static final ColumnExp FURNI_CONSTRAINT_C =
        new ColumnExp(PrizeRecord.class, FURNI_CONSTRAINT);
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1 + BASE_SCHEMA_VERSION * BASE_MULTIPLIER;

    /** The item type of the target prize item. */
    public byte targetType;

    /** The catalog id of the target prize item's listing. */
    public int targetCatalogId;

    @Override // from ItemRecord
    public byte getType ()
    {
        return Item.PRIZE;
    }

    @Override // from ItemRecord
    public void fromItem (Item item)
    {
        super.fromItem(item);

        Prize source = (Prize)item;
        targetType = source.targetType;
        targetCatalogId = source.targetCatalogId;
    }

    @Override // from ItemRecord
    protected Item createItem ()
    {
        Prize object = new Prize();
        object.targetType = targetType;
        object.targetCatalogId = targetCatalogId;
        return object;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link PrizeRecord}
     * with the supplied key values.
     */
    public static Key<PrizeRecord> getKey (int itemId)
    {
        return new Key<PrizeRecord>(
                PrizeRecord.class,
                new String[] { ITEM_ID },
                new Comparable[] { itemId });
    }
    // AUTO-GENERATED: METHODS END
}
