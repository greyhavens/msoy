//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.annotation.Column;
import com.samskivert.jdbc.depot.annotation.TableGenerator;
import com.samskivert.jdbc.depot.expression.ColumnExp;

/** Clone records for Avatars. */
@TableGenerator(name="cloneId", pkColumnValue="AVATAR_CLONE")
public class AvatarCloneRecord extends CloneRecord<AvatarRecord>
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #scale} field. */
    public static final String SCALE = "scale";

    /** The qualified column identifier for the {@link #scale} field. */
    public static final ColumnExp SCALE_C =
        new ColumnExp(AvatarCloneRecord.class, SCALE);
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
     * Create and return a primary {@link Key} to identify a {@link #AvatarCloneRecord}
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
