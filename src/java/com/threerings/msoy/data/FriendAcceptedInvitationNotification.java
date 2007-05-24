//
// $Id$

package com.threerings.msoy.data;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.util.ActionScript;

/**
 * Notification that gets sent to a user when an invited friend accepts the invitation.
 */
public class FriendAcceptedInvitationNotification extends Notification
{
    /** Friend's credentials. */
    public MemberName friend;
    
    /** Creates a new notification instance. */
    @ActionScript(omit=true)
    public FriendAcceptedInvitationNotification (MemberName friend)
    {
        this.friend = friend;
    }
}
