//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.annotation.Column;
import com.samskivert.jdbc.depot.annotation.TableGenerator;
import com.samskivert.jdbc.depot.expression.ColumnExp;

/** Clone records for Avatars. */
@TableGenerator(name="cloneId", pkColumnValue="AVATAR_CLONE")
public class AvatarCloneRecord extends CloneRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #scale} field. */
    public static final String SCALE = "scale";

    /** The qualified column identifier for the {@link #scale} field. */
    public static final ColumnExp SCALE_C =
        new ColumnExp(AvatarCloneRecord.class, SCALE);

    /** The qualified column identifier for the {@link #itemId} field. */
    public static final ColumnExp ITEM_ID_C =
        new ColumnExp(AvatarCloneRecord.class, ITEM_ID);

    /** The qualified column identifier for the {@link #originalItemId} field. */
    public static final ColumnExp ORIGINAL_ITEM_ID_C =
        new ColumnExp(AvatarCloneRecord.class, ORIGINAL_ITEM_ID);

    /** The qualified column identifier for the {@link #ownerId} field. */
    public static final ColumnExp OWNER_ID_C =
        new ColumnExp(AvatarCloneRecord.class, OWNER_ID);

    /** The qualified column identifier for the {@link #purchaseTime} field. */
    public static final ColumnExp PURCHASE_TIME_C =
        new ColumnExp(AvatarCloneRecord.class, PURCHASE_TIME);

    /** The qualified column identifier for the {@link #flowPaid} field. */
    public static final ColumnExp FLOW_PAID_C =
        new ColumnExp(AvatarCloneRecord.class, FLOW_PAID);

    /** The qualified column identifier for the {@link #goldPaid} field. */
    public static final ColumnExp GOLD_PAID_C =
        new ColumnExp(AvatarCloneRecord.class, GOLD_PAID);

    /** The qualified column identifier for the {@link #used} field. */
    public static final ColumnExp USED_C =
        new ColumnExp(AvatarCloneRecord.class, USED);

    /** The qualified column identifier for the {@link #location} field. */
    public static final ColumnExp LOCATION_C =
        new ColumnExp(AvatarCloneRecord.class, LOCATION);

    /** The qualified column identifier for the {@link #lastTouched} field. */
    public static final ColumnExp LAST_TOUCHED_C =
        new ColumnExp(AvatarCloneRecord.class, LAST_TOUCHED);

    /** The qualified column identifier for the {@link #name} field. */
    public static final ColumnExp NAME_C =
        new ColumnExp(AvatarCloneRecord.class, NAME);

    /** The qualified column identifier for the {@link #mediaHash} field. */
    public static final ColumnExp MEDIA_HASH_C =
        new ColumnExp(AvatarCloneRecord.class, MEDIA_HASH);

    /** The qualified column identifier for the {@link #mediaStamp} field. */
    public static final ColumnExp MEDIA_STAMP_C =
        new ColumnExp(AvatarCloneRecord.class, MEDIA_STAMP);
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1 +
        BASE_SCHEMA_VERSION * BASE_MULTIPLIER;

    /** The scale to apply to the avatar. */
    @Column(defaultValue="1")
    public float scale;

    @Override
    public void initialize (ItemRecord parent, int newOwnerId, int flowPaid, int goldPaid)
    {
        super.initialize(parent, newOwnerId, flowPaid, goldPaid);

        // and copy the parent's scale
        this.scale = ((AvatarRecord) parent).scale;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link AvatarCloneRecord}
     * with the supplied key values.
     */
    public static Key<AvatarCloneRecord> getKey (int itemId)
    {
        return new Key<AvatarCloneRecord>(
                AvatarCloneRecord.class,
                new String[] { ITEM_ID },
                new Comparable[] { itemId });
    }
    // AUTO-GENERATED: METHODS END
}
