//
// $Id$

package com.threerings.msoy.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.*; // for Depot annotations
import com.samskivert.jdbc.depot.expression.ColumnExp;

/**
 * Represents a friendship between two members.
 */
@Entity(indices={
    @Index(name="ixInviterId", fields={ FriendRecord.INVITER_ID }),
    @Index(name="ixInviteeId", fields={ FriendRecord.INVITEE_ID })
}, uniqueConstraints = {
    @UniqueConstraint(fieldNames={ FriendRecord.INVITER_ID, FriendRecord.INVITEE_ID })
})
public class FriendRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #inviterId} field. */
    public static final String INVITER_ID = "inviterId";

    /** The qualified column identifier for the {@link #inviterId} field. */
    public static final ColumnExp INVITER_ID_C =
        new ColumnExp(FriendRecord.class, INVITER_ID);

    /** The column identifier for the {@link #inviteeId} field. */
    public static final String INVITEE_ID = "inviteeId";

    /** The qualified column identifier for the {@link #inviteeId} field. */
    public static final ColumnExp INVITEE_ID_C =
        new ColumnExp(FriendRecord.class, INVITEE_ID);
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 3;

    /** The member id of the inviter. */
    @Id
    public int inviterId;

    /** The member id of the invitee. */
    @Id
    public int inviteeId;

    /**
     * Returns the member of this friendship that was not passed in as an argument.
     */
    public int getFriendId (int memberId)
    {
        return (inviterId == memberId) ? inviteeId : inviterId;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #FriendRecord}
     * with the supplied key values.
     */
    public static Key<FriendRecord> getKey (int inviterId, int inviteeId)
    {
        return new Key<FriendRecord>(
                FriendRecord.class,
                new String[] { INVITER_ID, INVITEE_ID },
                new Comparable[] { inviterId, inviteeId });
    }
    // AUTO-GENERATED: METHODS END
}
