//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Timestamp;

import com.google.common.base.Function;

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

    /** The last time bars and/or special items have been granted to this subscriber,
     *  which happens on a monthly basis. */
    // TODO: this will probably get more complicated for special items.
    // The special items are granted to all subscribers when they are released, and to any
    // new subscriber who has not yet received the item. So probably we just want a ItemIdent
    // indicating the last special item granted to each subscriber.
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
        return new Key<SubscriptionRecord>(
                SubscriptionRecord.class,
                new ColumnExp[] { MEMBER_ID },
                new Comparable[] { memberId });
    }
    // AUTO-GENERATED: METHODS END

    /** A functiont that turns a key back into a memberId. */
    public static Function<Key<SubscriptionRecord>,Integer> KEY_TO_MEMBER_ID =
        new Function<Key<SubscriptionRecord>,Integer>() {
            public Integer apply (Key<SubscriptionRecord> key) {
                return (Integer) key.getValues()[0];
            }
        };
}
