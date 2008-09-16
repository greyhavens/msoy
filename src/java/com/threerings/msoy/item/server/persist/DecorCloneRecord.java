//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.expression.ColumnExp;
import com.samskivert.jdbc.depot.annotation.TableGenerator;

/** Clone records for Decor. */
@TableGenerator(name="cloneId", pkColumnValue="DECOR_CLONE")
public class DecorCloneRecord extends CloneRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The qualified column identifier for the {@link #itemId} field. */
    public static final ColumnExp ITEM_ID_C =
        new ColumnExp(DecorCloneRecord.class, ITEM_ID);

    /** The qualified column identifier for the {@link #originalItemId} field. */
    public static final ColumnExp ORIGINAL_ITEM_ID_C =
        new ColumnExp(DecorCloneRecord.class, ORIGINAL_ITEM_ID);

    /** The qualified column identifier for the {@link #ownerId} field. */
    public static final ColumnExp OWNER_ID_C =
        new ColumnExp(DecorCloneRecord.class, OWNER_ID);

    /** The qualified column identifier for the {@link #purchaseTime} field. */
    public static final ColumnExp PURCHASE_TIME_C =
        new ColumnExp(DecorCloneRecord.class, PURCHASE_TIME);

    /** The qualified column identifier for the {@link #flowPaid} field. */
    public static final ColumnExp FLOW_PAID_C =
        new ColumnExp(DecorCloneRecord.class, FLOW_PAID);

    /** The qualified column identifier for the {@link #goldPaid} field. */
    public static final ColumnExp GOLD_PAID_C =
        new ColumnExp(DecorCloneRecord.class, GOLD_PAID);

    /** The qualified column identifier for the {@link #used} field. */
    public static final ColumnExp USED_C =
        new ColumnExp(DecorCloneRecord.class, USED);

    /** The qualified column identifier for the {@link #location} field. */
    public static final ColumnExp LOCATION_C =
        new ColumnExp(DecorCloneRecord.class, LOCATION);

    /** The qualified column identifier for the {@link #lastTouched} field. */
    public static final ColumnExp LAST_TOUCHED_C =
        new ColumnExp(DecorCloneRecord.class, LAST_TOUCHED);

    /** The qualified column identifier for the {@link #name} field. */
    public static final ColumnExp NAME_C =
        new ColumnExp(DecorCloneRecord.class, NAME);

    /** The qualified column identifier for the {@link #mediaHash} field. */
    public static final ColumnExp MEDIA_HASH_C =
        new ColumnExp(DecorCloneRecord.class, MEDIA_HASH);

    /** The qualified column identifier for the {@link #mediaStamp} field. */
    public static final ColumnExp MEDIA_STAMP_C =
        new ColumnExp(DecorCloneRecord.class, MEDIA_STAMP);
    // AUTO-GENERATED: FIELDS END

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link DecorCloneRecord}
     * with the supplied key values.
     */
    public static Key<DecorCloneRecord> getKey (int itemId)
    {
        return new Key<DecorCloneRecord>(
                DecorCloneRecord.class,
                new String[] { ITEM_ID },
                new Comparable[] { itemId });
    }
    // AUTO-GENERATED: METHODS END
}
