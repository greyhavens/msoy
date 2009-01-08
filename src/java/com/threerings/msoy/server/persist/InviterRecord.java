//
// $Id$

package com.threerings.msoy.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.*; // for Depot annotations
import com.samskivert.depot.expression.ColumnExp;

import com.samskivert.util.StringUtil;

/**
 * Contains persistent data stored for members that have been granted and/or have sent
 * invitations.
 */
@Entity
public class InviterRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<InviterRecord> _R = InviterRecord.class;
    public static final ColumnExp MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp INVITES_GRANTED = colexp(_R, "invitesGranted");
    public static final ColumnExp INVITES_SENT = colexp(_R, "invitesSent");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent
     * object in a way that will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** This inviter's unique id. */
    @Id
    public int memberId;

    /** The number of invites this inviter has available. */
    public int invitesGranted;

    /** The number of invites this inviter has used.  */
    public int invitesSent;

    /** A blank constructor used when loading records from the database. */
    public InviterRecord ()
    {
    }

    /** Generates a string representation of this instance. */
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link InviterRecord}
     * with the supplied key values.
     */
    public static Key<InviterRecord> getKey (int memberId)
    {
        return new Key<InviterRecord>(
                InviterRecord.class,
                new ColumnExp[] { MEMBER_ID },
                new Comparable[] { memberId });
    }
    // AUTO-GENERATED: METHODS END
}
