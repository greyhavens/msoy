//
// $Id$

package com.threerings.msoy.notify.data;

import com.threerings.util.ActionScript;
import com.threerings.util.MessageBundle;

/**
 * Notifies a user that they have been requested to play a game
 */
public class GameInviteNotification extends Notification
{
    public GameInviteNotification ()
    {
    }

    @ActionScript(omit=true)
    public GameInviteNotification (String inviter, int inviterId, String game, int gameId)
    {
        _inviter = inviter;
        _inviterId = inviterId;
        _game = game;
        _gameId = gameId;
    }

    // from Notification
    public String getAnnouncement ()
    {
        return MessageBundle.tcompose("m.game_invite", _inviter, _inviterId, _game, _gameId);
    }

    protected String _inviter;
    protected int _inviterId;
    protected String _game;
    protected int _gameId;
}
