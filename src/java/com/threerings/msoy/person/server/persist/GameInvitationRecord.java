//
// $Id$

package com.threerings.msoy.person.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.web.gwt.Invitation;

@Entity
public class GameInvitationRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<GameInvitationRecord> _R = GameInvitationRecord.class;
    public static final ColumnExp<String> INVITEE_EMAIL = colexp(_R, "inviteeEmail");
    public static final ColumnExp<String> INVITE_ID = colexp(_R, "inviteId");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 2;

    /** The email address we sent this invitation to. */
    @Id public String inviteeEmail;

    /** A randomly generated string of numbers and characters that is used to uniquely identify
     * this invitation. */
    @Index public String inviteId;

    /**
     * Converts this persistent record to a runtime record. Note game invitations do not record
     * the inviter since they are primarily for opting out securely.
     */
    public Invitation toInvitation ()
    {
        Invitation inv = new Invitation();
        inv.inviteeEmail = inviteeEmail;
        inv.inviteId = inviteId;
        return inv;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link GameInvitationRecord}
     * with the supplied key values.
     */
    public static Key<GameInvitationRecord> getKey (String inviteeEmail)
    {
        return newKey(_R, inviteeEmail);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(INVITEE_EMAIL); }
    // AUTO-GENERATED: METHODS END
}
