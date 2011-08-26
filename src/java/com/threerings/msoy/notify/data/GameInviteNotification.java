//
// $Id$

package com.threerings.msoy.notify.data;

import com.threerings.util.ActionScript;
import com.threerings.util.MessageBundle;

import com.threerings.orth.notify.data.Notification;

import com.threerings.msoy.data.all.MemberName;

/**
 * Notifies a user that they have been requested to play a game
 */
@com.threerings.util.ActionScript(omit=true)
public class GameInviteNotification extends Notification
{
    public GameInviteNotification ()
    {
    }

    @ActionScript(omit=true)
    public GameInviteNotification (MemberName inviter, String game, int gameId)
    {
        _inviter = inviter.toMemberName();
        _game = game;
        _gameId = gameId;
    }

    // from Notification
    public String getAnnouncement ()
    {
        return MessageBundle.tcompose("m.game_invite",
            _inviter, _inviter.getId(), _game, _gameId);
    }

    @Override
    public MemberName getSender ()
    {
        return _inviter;
    }

    protected MemberName _inviter;
    protected String _game;
    protected int _gameId;
}
