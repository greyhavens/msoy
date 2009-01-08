//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.annotation.TableGenerator;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.money.data.all.Currency;

/** Clone records for ItemPacks. */
@TableGenerator(name="cloneId", pkColumnValue="ITEMPACK_CLONE")
public class ItemPackCloneRecord extends CloneRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<ItemPackCloneRecord> _R = ItemPackCloneRecord.class;
    public static final ColumnExp ITEM_ID = colexp(_R, "itemId");
    public static final ColumnExp ORIGINAL_ITEM_ID = colexp(_R, "originalItemId");
    public static final ColumnExp OWNER_ID = colexp(_R, "ownerId");
    public static final ColumnExp PURCHASE_TIME = colexp(_R, "purchaseTime");
    public static final ColumnExp CURRENCY = colexp(_R, "currency");
    public static final ColumnExp AMOUNT_PAID = colexp(_R, "amountPaid");
    public static final ColumnExp USED = colexp(_R, "used");
    public static final ColumnExp LOCATION = colexp(_R, "location");
    public static final ColumnExp LAST_TOUCHED = colexp(_R, "lastTouched");
    public static final ColumnExp NAME = colexp(_R, "name");
    public static final ColumnExp MEDIA_HASH = colexp(_R, "mediaHash");
    public static final ColumnExp MEDIA_STAMP = colexp(_R, "mediaStamp");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1 + BASE_SCHEMA_VERSION * BASE_MULTIPLIER;

    @Override
    public void initialize (ItemRecord parent, int newOwnerId, Currency currency, int amountPaid)
    {
        super.initialize(parent, newOwnerId, currency, amountPaid);

        // TODO: copy anything needed from the original
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link ItemPackCloneRecord}
     * with the supplied key values.
     */
    public static Key<ItemPackCloneRecord> getKey (int itemId)
    {
        return new Key<ItemPackCloneRecord>(
                ItemPackCloneRecord.class,
                new ColumnExp[] { ITEM_ID },
                new Comparable[] { itemId });
    }
    // AUTO-GENERATED: METHODS END
}
