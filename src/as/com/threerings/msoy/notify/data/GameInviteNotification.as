//
// $Id$

package com.threerings.msoy.notify.data {

import com.threerings.io.ObjectInputStream;

import com.threerings.util.MessageBundle;

import com.threerings.msoy.data.all.MemberName;

/**
 * Notifies a user they have been requested to follow someone
 */
public class GameInviteNotification extends Notification
{
    // from Notification
    override public function getAnnouncement () :String
    {
        return MessageBundle.tcompose("m.game_invite",
            _inviter, _inviter.getMemberId(), _game, _gameId);
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
        _game = ins.readField(String) as String;
        _gameId = ins.readInt();
    }

    protected var _inviter :MemberName;
    protected var _game :String;
    protected var _gameId :int;
}
}
