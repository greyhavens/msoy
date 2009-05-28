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
    public static final ColumnExp END_DATE = colexp(_R, "endDate");
    public static final ColumnExp LAST_GRANT = colexp(_R, "lastGrant");
    // AUTO-GENERATED: FIELDS END

    /** To be updated when the structure of this class changes. */
    public static final int SCHEMA_VERSION = 1;

    /** The memberId of the subscriber. */
    @Id
    public int memberId;

    /** The time at which the subscription ends. This may be in the past if this
     * represents an expired subscription. It is useful to us to see people that are
     * ex-subscribers. */
    public Timestamp endDate;

    /** The last time bars and/or special items have been granted to this subscriber,
     *  which happens on a monthly basis. */
    // TODO: this will probably get more complicated for special items.
    // The special items are granted to all subscribers when they are released, and to any
    // new subscriber who has not yet received the item. So probably we just want a ItemIdent
    // indicating the last special item granted to each subscriber.
    public Timestamp lastGrant;

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
}
