//
// $Id$

package com.threerings.msoy.game.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.threerings.msoy.item.data.all.MsoyItemType;
import com.threerings.presents.peer.data.NodeObject;

import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.server.BodyManager;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.VizMemberName;

import com.threerings.msoy.peer.data.MsoyNodeObject;
import com.threerings.msoy.peer.server.MemberNodeAction;
import com.threerings.msoy.peer.server.MsoyPeerManager;

import com.threerings.msoy.game.data.GameAuthName;
import com.threerings.msoy.game.data.GameSummary;
import com.threerings.msoy.game.data.PlayerObject;

/**
 * Provides a simple interface for dispatching node actions for players.
 */
@Singleton
public class PlayerNodeActions
{
    public void updatePlayerGame (PlayerObject plobj, GameSummary game)
    {
        if (plobj.game == null || plobj.game.gameId != game.gameId) {
            plobj.setGame(game);
            if (_peerMan.updateMemberGame(plobj.getMemberId(), game)) {
                _peerMan.invokeNodeAction(new UpdatePlayerAction(plobj.getMemberId(), game));
            }
        }
    }

    public void clearPlayerGame (int playerId)
    {
        _peerMan.updateMemberGame(playerId, null);
        _peerMan.invokeNodeAction(new UpdatePlayerAction(playerId, null));
    }

    public void displayNameUpdated (MemberName name)
    {
        _peerMan.invokeNodeAction(new DisplayNameUpdated(name));
    }

    public void gameContentPurchased (int playerId, int gameId, MsoyItemType itemType, String ident)
    {
        _peerMan.invokeNodeAction(new ContentPurchasedAction(playerId, gameId, itemType, ident));
    }

    public void flushCoins (int playerId)
    {
        _peerMan.invokeNodeAction(new FlushCoinsAction(playerId));
    }

    protected static abstract class PlayerNodeAction extends MsoyPeerManager.NodeAction
    {
        public PlayerNodeAction (int playerId) {
            _playerId = playerId;
        }

        public PlayerNodeAction () {
        }

        @Override // from PeerManager.NodeAction
        public boolean isApplicable (NodeObject nodeobj) {
            return ((MsoyNodeObject)nodeobj).clients.containsKey(GameAuthName.makeKey(_playerId));
        }

        @Override // from PeerManager.NodeAction
        protected void execute () {
            PlayerObject plobj = _locator.lookupPlayer(_playerId);
            if (plobj != null) {
                execute(plobj);
            } // if not, oh well, they went away
        }

        protected abstract void execute (PlayerObject plobj);

        protected int _playerId;

        /** Used to look up member objects. */
        @Inject protected transient PlayerLocator _locator;
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
            _gameReg.updatePlayerGame(memObj, _game);
        }

        protected GameSummary _game;
        @Inject protected transient WorldGameRegistry _gameReg;
    }

    /** Handles informing a game server that a player's display name has changed. */
    protected static class DisplayNameUpdated extends PlayerNodeAction
    {
        public DisplayNameUpdated (MemberName name) {
            super(name.getId());
            _name = name.toString();
        }

        public DisplayNameUpdated () {
        }

        protected void execute (PlayerObject plobj) {
            // update their player object
            plobj.setMemberName(
                new VizMemberName(_name, plobj.getMemberId(), plobj.getHeadShotMedia()));

            // update their name in their occupied "place" (game) if any
            _bodyMan.updateOccupantInfo(
                plobj, new OccupantInfo.NameUpdater(plobj.getVisibleName()));
        }

        protected String _name;
        @Inject protected transient BodyManager _bodyMan;
    }

    /** Notifies other nodes when a user has purchased game content. */
    protected static class ContentPurchasedAction extends PlayerNodeAction
    {
        public ContentPurchasedAction (int memberId, int gameId, MsoyItemType itemType, String ident) {
            super(memberId);
            _gameId = gameId;
            _itemType = itemType;
            _ident = ident;
        }

        public ContentPurchasedAction () {
        }

        @Override protected void execute (PlayerObject plobj) {
            _gameReg.gameContentPurchased(plobj, _gameId, _itemType, _ident);
        }

        protected int _gameId;
        protected MsoyItemType _itemType;
        protected String _ident;
        @Inject protected transient GameGameRegistry _gameReg;
    }

    protected static class FlushCoinsAction extends PlayerNodeAction
    {
        public FlushCoinsAction (int playerId) {
            super(playerId);
        }

        public FlushCoinsAction () {
        }

        protected void execute (PlayerObject plobj) {
            _gameReg.flushCoinEarnings(plobj);
        }

        @Inject protected transient GameGameRegistry _gameReg;
    }

    @Inject protected MsoyPeerManager _peerMan;
}
