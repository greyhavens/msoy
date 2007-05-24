//
// $Id$

package com.threerings.msoy.data;

import com.threerings.util.ActionScript;

/**
 * Notification that gets sent to a user when an invited friend accepts the invitation.
 */
public class FriendAcceptedInvitationNotification extends Notification
{
    /** Invited friend's screen name. */
    public String inviteeDisplayName;

    /** Friend's email address, to which the invitation was sent. */
    public String invitationEmail; 
    
    /** Creates a new notification instance. */
    @ActionScript(omit=true)
    public FriendAcceptedInvitationNotification (String displayName, String email)
    {
        this.inviteeDisplayName = displayName;
        this.invitationEmail = email;
    }
}
