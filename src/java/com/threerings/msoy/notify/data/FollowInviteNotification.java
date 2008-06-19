//
// $Id$

package com.threerings.msoy.notify.data;

import com.threerings.util.ActionScript;
import com.threerings.util.MessageBundle;

/**
 * Notifies a user that they have been requested to play a game
 */
public class FollowInviteNotification extends Notification
{
    public FollowInviteNotification ()
    {
    }

    @ActionScript(omit=true)
    public FollowInviteNotification (String inviter, int inviterId)
    {
        _inviter = inviter;
        _inviterId = inviterId;
    }

    // from Notification
    public String getAnnouncement ()
    {
        return MessageBundle.tcompose("m.follow_invite", _inviter, _inviterId);
    }

    protected String _inviter;
    protected int _inviterId;
}
