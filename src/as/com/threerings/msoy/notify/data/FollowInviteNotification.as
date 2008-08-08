//
// $Id$

package com.threerings.msoy.notify.data {

import com.threerings.io.ObjectInputStream;

import com.threerings.util.MessageBundle;

/**
 * Notifies a user they have been requested to follow someone
 */
public class FollowInviteNotification extends Notification
{
    // from Notification
    override public function getAnnouncement () :String
    {
        return MessageBundle.tcompose("m.follow_invite", _inviter, _inviterId);
    }

    // from Notification
    override public function getCategory () :int
    {
        return INVITE;
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        _inviter = ins.readField(String) as String;
        _inviterId = ins.readInt();
    }

    protected var _inviter :String;
    protected var _inviterId :int;
}
}
