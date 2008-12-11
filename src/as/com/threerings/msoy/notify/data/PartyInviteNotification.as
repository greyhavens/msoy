//
// $Id$

package com.threerings.msoy.notify.data {

import com.threerings.util.MessageBundle;

import com.threerings.io.ObjectInputStream;

import com.threerings.msoy.data.all.MemberName;

public class PartyInviteNotification extends Notification
{
    override public function getAnnouncement () :String
    {
        return MessageBundle.tcompose("m.party_invite",
            _inviter, _inviter.getMemberId(), _partyName, _partyId);
    }

    override public function getCategory () :int
    {
        return INVITE;
    }

    override public function getSender () :MemberName
    {
        return _inviter;
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        _inviter = MemberName(ins.readObject());
        _partyId = ins.readInt();
        _partyName = ins.readField(String) as String;
    }

    protected var _inviter :MemberName;
    protected var _partyId :int;
    protected var _partyName :String;
}
}
