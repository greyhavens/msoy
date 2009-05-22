//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Timestamp;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Records a payout that occurred due to bringing a friend to Whirled (and that friend meeting some
 * other criteria).
 */
@Entity
public class FriendPayoutRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<FriendPayoutRecord> _R = FriendPayoutRecord.class;
    public static final ColumnExp FRIEND_ID = colexp(_R, "friendId");
    public static final ColumnExp PAID_MEMBER_ID = colexp(_R, "paidMemberId");
    public static final ColumnExp TIME = colexp(_R, "time");
    // AUTO-GENERATED: FIELDS END

    /** The id of the friend who was harvested. */
    @Id public int friendId;

    /** The id of the member who was paid. */
    public int paidMemberId;

    /** The time the payout was made. */
    @Column(defaultValue="CURRENT_TIMESTAMP")
    public Timestamp time;

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 2;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link FriendPayoutRecord}
     * with the supplied key values.
     */
    public static Key<FriendPayoutRecord> getKey (int friendId)
    {
        return new Key<FriendPayoutRecord>(
                FriendPayoutRecord.class,
                new ColumnExp[] { FRIEND_ID },
                new Comparable[] { friendId });
    }
    // AUTO-GENERATED: METHODS END
}
