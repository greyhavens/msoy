//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Timestamp;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.*; // for Depot annotations
import com.samskivert.depot.expression.ColumnExp;

/**
 * Tracks data about subscribers and ex-subscribers.
 */
public class SubscriptionRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<SubscriptionRecord> _R = SubscriptionRecord.class;
    public static final ColumnExp MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp SUBSCRIBER = colexp(_R, "subscriber");
    public static final ColumnExp GRANTS_LEFT = colexp(_R, "grantsLeft");
    public static final ColumnExp LAST_GRANT = colexp(_R, "lastGrant");
    public static final ColumnExp SPECIAL_ITEM_TYPE = colexp(_R, "specialItemType");
    public static final ColumnExp SPECIAL_ITEM_ID = colexp(_R, "specialItemId");
    // AUTO-GENERATED: FIELDS END

    /** To be updated when the structure of this class changes. */
    public static final int SCHEMA_VERSION = 2;

    /** The memberId of the subscriber. */
    @Id
    public int memberId;

    /** Are they currently a subscriber? */
    public boolean subscriber;

    /** How many bar grants do they have left? */
    public int grantsLeft;

    /** The last time bars (only) have been granted to this subscriber,
     *  which happens on a monthly basis. Item grants are tracked separately. */
    @Column(nullable=true)
    public Timestamp lastGrant;

    /** The type of the last special item granted to this subscriber. */
    public byte specialItemType;

    /** The item id of the last special item granted to this subscriber. */
    public int specialItemId;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link SubscriptionRecord}
     * with the supplied key values.
     */
    public static Key<SubscriptionRecord> getKey (int memberId)
    {
        return newKey(_R, memberId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(MEMBER_ID); }
    // AUTO-GENERATED: METHODS END
}
