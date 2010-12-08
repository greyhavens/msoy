//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.sql.Timestamp;

import com.samskivert.depot.Key;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.annotation.TableGenerator;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.money.data.all.Currency;

/** Clone records for Documents. */
@TableGenerator(name="cloneId", pkColumnValue="DOCUMENT_CLONE")
public class DocumentCloneRecord extends CloneRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<DocumentCloneRecord> _R = DocumentCloneRecord.class;
    public static final ColumnExp<Integer> ITEM_ID = colexp(_R, "itemId");
    public static final ColumnExp<Integer> ORIGINAL_ITEM_ID = colexp(_R, "originalItemId");
    public static final ColumnExp<Integer> OWNER_ID = colexp(_R, "ownerId");
    public static final ColumnExp<Timestamp> PURCHASE_TIME = colexp(_R, "purchaseTime");
    public static final ColumnExp<Currency> CURRENCY = colexp(_R, "currency");
    public static final ColumnExp<Integer> AMOUNT_PAID = colexp(_R, "amountPaid");
    public static final ColumnExp<Item.UsedAs> USED = colexp(_R, "used");
    public static final ColumnExp<Integer> LOCATION = colexp(_R, "location");
    public static final ColumnExp<Timestamp> LAST_TOUCHED = colexp(_R, "lastTouched");
    public static final ColumnExp<String> NAME = colexp(_R, "name");
    public static final ColumnExp<byte[]> MEDIA_HASH = colexp(_R, "mediaHash");
    public static final ColumnExp<Timestamp> MEDIA_STAMP = colexp(_R, "mediaStamp");
    // AUTO-GENERATED: FIELDS END

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link DocumentCloneRecord}
     * with the supplied key values.
     */
    public static Key<DocumentCloneRecord> getKey (int itemId)
    {
        return newKey(_R, itemId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(ITEM_ID); }
    // AUTO-GENERATED: METHODS END
}
