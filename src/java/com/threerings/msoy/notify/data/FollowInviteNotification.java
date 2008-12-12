//
// $Id$

package com.threerings.msoy.notify.data;

import com.threerings.util.ActionScript;
import com.threerings.util.MessageBundle;

import com.threerings.msoy.data.all.MemberName;

/**
 * Notifies a user that they have been requested to play a game
 */
public class FollowInviteNotification extends Notification
{
    public FollowInviteNotification ()
    {
    }

    @ActionScript(omit=true)
    public FollowInviteNotification (MemberName inviter)
    {
        _inviter = inviter.toMemberName();
    }

    // from Notification
    public String getAnnouncement ()
    {
        return MessageBundle.tcompose("m.follow_invite", _inviter, _inviter.getMemberId());
    }

    @Override
    public MemberName getSender ()
    {
        return _inviter;
    }

    protected MemberName _inviter;
}
