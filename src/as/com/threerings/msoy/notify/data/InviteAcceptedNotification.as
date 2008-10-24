//
// $Id$

package com.threerings.msoy.notify.data {

import com.threerings.io.ObjectInputStream;

import com.threerings.util.MessageBundle;

import com.threerings.msoy.data.all.MemberName;

/**
 * Notifies a user that an invitation was accepted
 */
public class InviteAcceptedNotification extends Notification
{
    // from Notification
    override public function getAnnouncement () :String
    {
        return MessageBundle.tcompose(
            "m.invite_accepted", _inviteeEmail, _invitee, _invitee.getMemberId());
    }

    // from Notification
    override public function getCategory () :int
    {
        return PERSONAL;
    }

    // from Notification
    override public function getSender () :MemberName
    {
        return _invitee;
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        _invitee = MemberName(ins.readObject());
        _inviteeEmail = ins.readField(String) as String;
    }

    protected var _invitee :MemberName;
    protected var _inviteeEmail :String;
}
}
