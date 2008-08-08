//
// $Id$

package com.threerings.msoy.notify.data {

import com.threerings.io.ObjectInputStream;

import com.threerings.util.MessageBundle;

/**
 * Notifies a user that an invitation was accepted
 */
public class InviteAcceptedNotification extends Notification
{
    // from Notification
    override public function getAnnouncement () :String
    {
        return MessageBundle.tcompose(
            "m.invite_accepted", _inviteeEmail, _inviteeDisplayName, _inviteeId);
    }

    // from Notification
    override public function getCategory () :int
    {
        return PERSONAL;
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        _inviteeEmail = ins.readField(String) as String;
        _inviteeDisplayName = ins.readField(String) as String;
        _inviteeId = ins.readInt();
    }

    protected var _inviteeEmail :String;
    protected var _inviteeDisplayName :String;
    protected var _inviteeId :int;
}
}
