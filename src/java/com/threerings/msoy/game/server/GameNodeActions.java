//
// $Id$

package com.threerings.msoy.game.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.threerings.presents.peer.data.NodeObject;

import com.threerings.msoy.peer.data.MsoyNodeObject;
import com.threerings.msoy.peer.server.MsoyPeerManager;

/**
 * Game-related node actions.
 */
@Singleton
public class GameNodeActions
{
    /**
     * Notifies other nodes when a game or its content is updated.
     */
    public void gameUpdated (int gameId)
    {
        _peerMan.invokeNodeAction(new GameUpdatedAction(gameId));
    }

    public void resetScores (int gameId, boolean single, int gameMode)
    {
        _peerMan.invokeNodeAction(new ResetScoresAction(gameId, single, gameMode));
    }

    protected static abstract class GameNodeAction extends MsoyPeerManager.NodeAction
    {
        public GameNodeAction (int gameId) {
            _gameId = gameId;
        }

        public GameNodeAction () {
        }

        @Override // from PeerManager.NodeAction
        public boolean isApplicable (NodeObject nodeobj) {
            return ((MsoyNodeObject)nodeobj).hostedGames.containsKey(_gameId);
        }

        protected int _gameId;
    }

    protected static class GameUpdatedAction extends GameNodeAction
    {
        public GameUpdatedAction (int gameId) {
            super(gameId);
        }

        public GameUpdatedAction () {
        }

        @Override protected void execute () {
            _gameReg.gameContentUpdated(_gameId);
        }

        @Inject protected transient GameGameRegistry _gameReg;
    }

    protected static class ResetScoresAction extends GameNodeAction
    {
        public ResetScoresAction (int gameId, boolean single, int gameMode) {
            super(gameId);
            _single = single;
            _gameMode = gameMode;
        }

        public ResetScoresAction () {
        }

        @Override // from PeerManager.NodeAction
        protected void execute () {
            _gameReg.resetScorePercentiler(_gameId, _single, _gameMode);
        }

        protected boolean _single;
        protected int _gameMode;

        @Inject protected transient GameGameRegistry _gameReg;
    }

    @Inject protected MsoyPeerManager _peerMan;
}
