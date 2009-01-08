//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Timestamp;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.*; // for Depot annotations
import com.samskivert.depot.expression.ColumnExp;

import com.samskivert.util.StringUtil;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.gwt.Invitation;

/**
 * Contains persistent data stored for every member of MetaSOY.
 */
@Entity
public class InvitationRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<InvitationRecord> _R = InvitationRecord.class;
    public static final ColumnExp INVITEE_EMAIL = colexp(_R, "inviteeEmail");
    public static final ColumnExp INVITER_ID = colexp(_R, "inviterId");
    public static final ColumnExp INVITE_ID = colexp(_R, "inviteId");
    public static final ColumnExp INVITEE_ID = colexp(_R, "inviteeId");
    public static final ColumnExp ISSUED = colexp(_R, "issued");
    public static final ColumnExp VIEWED = colexp(_R, "viewed");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 5;

    /** The email address we're sending this invitation to. */
    public String inviteeEmail;

    /** The inviter's member Id */
    @Index(name="ixInviter")
    public int inviterId;

    /** A randomly generated string of numbers and characters that is used to uniquely identify
     * this invitation. */
    @Id
    public String inviteId;

    /** The memberId that was assigned to the invitee when (if) the invitation was accepted. */
    @Index(name="ixInvitee")
    public int inviteeId;

    /** The time that this invite was sent out */
    public Timestamp issued;

    /** The time that this invitation was first viewed. */
    @Column(nullable=true)
    public Timestamp viewed;

    /** A blank constructor used when loading records from the database. */
    public InvitationRecord ()
    {
    }

    /**
     * Create a new record for an Invitation that is being issued right now.
     */
    public InvitationRecord (String inviteeEmail, int inviterId, String inviteId)
    {
        this.inviteeEmail = inviteeEmail;
        this.inviterId = inviterId;
        this.inviteId = inviteId;
        issued = new Timestamp((new java.util.Date()).getTime());
    }

    /** Generates a string representation of this instance. */
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }

    /**
     * Converts this persistent record to a runtime record.
     *
     * @param inviterName {@link #inviterId} converted to a {@link MemberName}.
     */
    public Invitation toInvitation (MemberName inviterName)
    {
        Invitation inv = new Invitation();
        inv.inviteeEmail = inviteeEmail;
        inv.inviteId = inviteId;
        inv.inviter = inviterName;
        return inv;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link InvitationRecord}
     * with the supplied key values.
     */
    public static Key<InvitationRecord> getKey (String inviteId)
    {
        return new Key<InvitationRecord>(
                InvitationRecord.class,
                new ColumnExp[] { INVITE_ID },
                new Comparable[] { inviteId });
    }
    // AUTO-GENERATED: METHODS END
}
