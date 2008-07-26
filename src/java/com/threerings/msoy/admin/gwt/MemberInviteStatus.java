//
// $Id$

package com.threerings.msoy.admin.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

public class MemberInviteStatus
    implements IsSerializable
{
    /** The member's memberId */
    public int memberId;

    /** The member's permaname, or if none his display name. */
    public String name;

    /** The id of the member that invited this person. */
    public int invitingFriendId;

    /** The number of invites this user has available. */
    public int invitesGranted;

    /** The number of invites this user has sent. */
    public int invitesSent;
}
