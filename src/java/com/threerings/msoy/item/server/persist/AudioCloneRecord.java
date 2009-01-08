//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.annotation.TableGenerator;

/** Clone records for Audios. */
@TableGenerator(name="cloneId", pkColumnValue="AUDIO_CLONE")
public class AudioCloneRecord extends CloneRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<AudioCloneRecord> _R = AudioCloneRecord.class;
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

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link AudioCloneRecord}
     * with the supplied key values.
     */
    public static Key<AudioCloneRecord> getKey (int itemId)
    {
        return new Key<AudioCloneRecord>(
                AudioCloneRecord.class,
                new ColumnExp[] { ITEM_ID },
                new Comparable[] { itemId });
    }
    // AUTO-GENERATED: METHODS END
}
