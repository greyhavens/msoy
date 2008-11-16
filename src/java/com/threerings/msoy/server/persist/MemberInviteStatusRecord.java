//
// $Id$

package com.threerings.msoy.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.*; // for Depot annotations
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.admin.gwt.MemberInviteStatus;

/**
 * A computed persistent entity that's used to fetch (and cache) information about members and
 * their invitations, as well as who invited them.
 */
@Entity
@Computed(shadowOf=MemberRecord.class)
public class MemberInviteStatusRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #memberId} field. */
    public static final String MEMBER_ID = "memberId";

    /** The qualified column identifier for the {@link #memberId} field. */
    public static final ColumnExp MEMBER_ID_C =
        new ColumnExp(MemberInviteStatusRecord.class, MEMBER_ID);

    /** The column identifier for the {@link #permaName} field. */
    public static final String PERMA_NAME = "permaName";

    /** The qualified column identifier for the {@link #permaName} field. */
    public static final ColumnExp PERMA_NAME_C =
        new ColumnExp(MemberInviteStatusRecord.class, PERMA_NAME);

    /** The column identifier for the {@link #name} field. */
    public static final String NAME = "name";

    /** The qualified column identifier for the {@link #name} field. */
    public static final ColumnExp NAME_C =
        new ColumnExp(MemberInviteStatusRecord.class, NAME);

    /** The column identifier for the {@link #affiliateMemberId} field. */
    public static final String AFFILIATE_MEMBER_ID = "affiliateMemberId";

    /** The qualified column identifier for the {@link #affiliateMemberId} field. */
    public static final ColumnExp AFFILIATE_MEMBER_ID_C =
        new ColumnExp(MemberInviteStatusRecord.class, AFFILIATE_MEMBER_ID);

    /** The column identifier for the {@link #invitesGranted} field. */
    public static final String INVITES_GRANTED = "invitesGranted";

    /** The qualified column identifier for the {@link #invitesGranted} field. */
    public static final ColumnExp INVITES_GRANTED_C =
        new ColumnExp(MemberInviteStatusRecord.class, INVITES_GRANTED);

    /** The column identifier for the {@link #invitesSent} field. */
    public static final String INVITES_SENT = "invitesSent";

    /** The qualified column identifier for the {@link #invitesSent} field. */
    public static final ColumnExp INVITES_SENT_C =
        new ColumnExp(MemberInviteStatusRecord.class, INVITES_SENT);
    // AUTO-GENERATED: FIELDS END

    /** See {@link MemberRecord#memberId}. */
    @Id
    public int memberId;

    /** See {@link MemberRecord#permaName}. */
    public String permaName = "";

    /** See {@link MemberRecord#name}. */
    public String name = "";

    // TODO: this needs fixing. You affiliate is not necessarily your inviter
    /** See {@link MemberRecord#affiliateMemberId}. */
    public int affiliateMemberId;

    /** See {@link InviterRecord#invitesGranted}. */
    @Computed(shadowOf=InviterRecord.class)
    public int invitesGranted;

    /** See {@link InviterRecord#invitesSent}. */
    @Computed(shadowOf=InviterRecord.class)
    public int invitesSent;

    public MemberInviteStatus toWebObject ()
    {
        MemberInviteStatus webObj = new MemberInviteStatus();
        webObj.memberId = memberId;
        webObj.name = permaName == null || permaName.equals("") ? name : permaName;
        webObj.invitingFriendId = affiliateMemberId; // TODO: not true. See note on field above.
        webObj.invitesGranted = invitesGranted;
        webObj.invitesSent = invitesSent;
        return webObj;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link MemberInviteStatusRecord}
     * with the supplied key values.
     */
    public static Key<MemberInviteStatusRecord> getKey (int memberId)
    {
        return new Key<MemberInviteStatusRecord>(
                MemberInviteStatusRecord.class,
                new String[] { MEMBER_ID },
                new Comparable[] { memberId });
    }
    // AUTO-GENERATED: METHODS END
}
