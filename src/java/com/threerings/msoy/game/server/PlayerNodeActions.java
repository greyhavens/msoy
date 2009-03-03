//
// $Id$

package com.threerings.msoy.game.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.peer.server.MemberNodeAction;
import com.threerings.msoy.peer.server.MsoyPeerManager;

import com.threerings.msoy.game.data.GameSummary;
import com.threerings.msoy.game.data.PlayerObject;

/**
 * Provides a simple interface for dispatching node actions for players.
 */
@Singleton
public class PlayerNodeActions
{
    public void updatePlayer (int playerId, GameSummary game)
    {
        _peerMan.invokeNodeAction(new UpdatePlayerAction(playerId, game));
    }

    public void leaveAVRGame (int playerId)
    {
        _peerMan.invokeNodeAction(new LeaveAVRGameAction(playerId));
    }

    public void displayNameUpdated (MemberName name)
    {
        _peerMan.invokeNodeAction(new DisplayNameUpdated(name));
    }

    /** Handles updating a player's game. */
    protected static class UpdatePlayerAction extends MemberNodeAction
    {
        public UpdatePlayerAction (int memberId, GameSummary game) {
            super(memberId);
            _game = game;
        }

        public UpdatePlayerAction () {
        }

        protected void execute (MemberObject memObj) {
            _gameReg.updatePlayerOnPeer(memObj, _game);
        }

        protected GameSummary _game;
        @Inject protected transient WorldGameRegistry _gameReg;
    }

    /** Handles leaving an AVR game. */
    protected static class LeaveAVRGameAction extends MemberNodeAction
    {
        public LeaveAVRGameAction (int memberId) {
            super(memberId);
        }

        public LeaveAVRGameAction () {
        }

        protected void execute (MemberObject memObj) {
            // clear their AVRG affiliation
            memObj.setAvrGameId(0);
        }
    }

    protected static class DisplayNameUpdated extends PlayerNodeAction
    {
        public DisplayNameUpdated (MemberName name) {
            super(name.getMemberId());
            _name = name.toString();
        }

        public DisplayNameUpdated () {
        }

        protected void execute (PlayerObject plobj) {
            // TODO
        }

        protected String _name;
    }

    @Inject protected MsoyPeerManager _peerMan;
}
