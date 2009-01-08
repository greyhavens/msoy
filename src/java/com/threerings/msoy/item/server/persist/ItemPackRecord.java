//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.annotation.TableGenerator;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemPack;

/**
 * Contains the persistent data for a ItemPack item.
 */
@TableGenerator(name="itemId", pkColumnValue="ITEMPACK")
public class ItemPackRecord extends SubItemRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<ItemPackRecord> _R = ItemPackRecord.class;
    public static final ColumnExp SUITE_ID = colexp(_R, "suiteId");
    public static final ColumnExp IDENT = colexp(_R, "ident");
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

    public static final int SCHEMA_VERSION = 3 + BASE_SCHEMA_VERSION * BASE_MULTIPLIER;

    @Override // from ItemRecord
    public byte getType ()
    {
        return Item.ITEM_PACK;
    }

    @Override // from ItemRecord
    public void fromItem (Item item)
    {
        super.fromItem(item);

        // ItemPack pack = (ItemPack)item;
        // nothing to do
    }

    @Override // from ItemRecord
    protected Item createItem ()
    {
        ItemPack object = new ItemPack();
        // nothing to do
        return object;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link ItemPackRecord}
     * with the supplied key values.
     */
    public static Key<ItemPackRecord> getKey (int itemId)
    {
        return new Key<ItemPackRecord>(
                ItemPackRecord.class,
                new ColumnExp[] { ITEM_ID },
                new Comparable[] { itemId });
    }
    // AUTO-GENERATED: METHODS END
}
