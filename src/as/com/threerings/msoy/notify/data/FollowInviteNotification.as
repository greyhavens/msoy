//
// $Id$

package com.threerings.msoy.notify.data {

import com.threerings.io.ObjectInputStream;

import com.threerings.util.MessageBundle;

import com.threerings.msoy.data.all.MemberName;

/**
 * Notifies a user they have been requested to follow someone
 */
public class FollowInviteNotification extends Notification
{
    // from Notification
    override public function getAnnouncement () :String
    {
        return MessageBundle.tcompose("m.follow_invite", _inviter, _inviter.getMemberId());
    }

    // from Notification
    override public function getCategory () :int
    {
        return INVITE;
    }

    // from Notification
    override public function getSender () :MemberName
    {
        return _inviter;
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        _inviter = MemberName(ins.readObject());
    }

    protected var _inviter :MemberName;
}
}
