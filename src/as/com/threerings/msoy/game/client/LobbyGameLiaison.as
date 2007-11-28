//
// $Id$

package com.threerings.msoy.game.client {

import com.threerings.util.Log;

import com.threerings.presents.client.ClientEvent;
import com.threerings.presents.client.ClientObserver;
import com.threerings.presents.client.ResultWrapper;

import com.threerings.crowd.client.LocationAdapter;
import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.parlor.client.GameReadyObserver;

import com.threerings.msoy.client.DeploymentConfig;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.ui.MsoyUI;

import com.threerings.msoy.game.data.LobbyCodes;
import com.threerings.msoy.game.data.MsoyGameConfig;

import mx.containers.HBox;

/**
 * Handles the lobby-specific aspects of the game server connection.
 */
public class LobbyGameLiaison extends GameLiaison
    implements GameReadyObserver
{
    public static const log :Log = Log.getLog(LobbyGameLiaison);

    public static const PLAY_NOW_SINGLE :int = LobbyCodes.PLAY_NOW_SINGLE;
    public static const PLAY_NOW_FRIENDS :int = LobbyCodes.PLAY_NOW_FRIENDS;
    public static const PLAY_NOW_ANYONE :int = LobbyCodes.PLAY_NOW_ANYONE;
    public static const SHOW_LOBBY :int = PLAY_NOW_ANYONE + 1;
    public static const JOIN_PLAYER :int = SHOW_LOBBY + 1;

    public function LobbyGameLiaison (ctx :WorldContext, gameId :int, mode :int, playerId :int = 0)
    {
        super(ctx, gameId);

        _mode = mode;
        _playerIdGame = playerId;

        log.info("Started game liaison [gameId=" + _gameId + ", mode=" + _mode + "].");

        // listen for our game to be ready so that we can display it
        _gctx.getParlorDirector().addGameReadyObserver(this);

        // listen for changes in world location so that we can shutdown if we move
        _ctx.getLocationDirector().addLocationObserver(_worldLocObs);

        // display feedback indicating that we're locating their game
        var loading :HBox = new HBox();
        loading.styleName = "lobbyLoadingBox";
        loading.width = LobbyPanel.LOBBY_PANEL_WIDTH;
        loading.percentHeight = 100;
        loading.addChild(MsoyUI.createLabel(Msgs.GAME.get("l.locating_game")));
        _ctx.getTopPanel().setLeftPanel(loading);
    }

    /**
     * Join the player in their running game if possible, otherwise simply display the game lobby,
     * if it isn't up already.
     */
    public function joinPlayer (playerId :int) :void
    {
        if (!_gctx.getClient().isLoggedOn()) {
            // this function will be called again, once we've logged onto the game server.
            _playerIdGame = playerId;
            return;
        }

        var lsvc :LobbyService = (_gctx.getClient().requireService(LobbyService) as LobbyService);
        var cb :ResultWrapper = new ResultWrapper(function (cause :String) :void {
            _ctx.displayFeedback(MsoyCodes.GAME_MSGS, cause);
            // some failure cases are innocuous, and should be followed up by a display of the 
            // lobby - if we really are hosed, joinLobby() will cause the liaison to shut down.
            _ctx.getMsoyController().restoreSceneURL();
            showLobbyUI();
        }, gotPlayerGameOid);
        lsvc.joinPlayerGame(_gctx.getClient(), playerId, cb);
    }

    /** 
     * Join the player at their currently pending game table.
     */
    public function joinPlayerTable (playerId :int) :void
    {
        if (_lobby == null) {
            // this function will be called again, once we've got our lobby
            _playerIdTable = playerId;
        } else {
            _lobby.joinPlayerTable(playerId);
        }
    }

    /**
     * Returns the config of our active game if we're in an active game.
     */
    public function getGameConfig () :MsoyGameConfig
    {
        var ctrl :PlaceController = _gctx.getLocationDirector().getPlaceController();
        return (ctrl == null) ? null : (ctrl.getPlaceConfig() as MsoyGameConfig);
    }

    public function showLobbyUI () :void
    {
        if (_lobby != null) {
            _lobby.restoreLobbyUI();
        } else {
            joinLobby();
        }
    }

    public function playNow (mode :int) :void
    {
        var lsvc :LobbyService = (_gctx.getClient().requireService(LobbyService) as LobbyService);
        var cb :ResultWrapper = new ResultWrapper(function (cause :String) :void {
            _enterNextGameDirect = false;
            _ctx.displayFeedback(MsoyCodes.GAME_MSGS, cause);
            shutdown();
        },
        function (result :Object) :void {
            if (int(result) != 0) {
                // we failed to start a game (see below) so join the lobby instead
                _enterNextGameDirect = false;
                gotLobbyOid(result);
            }
        });
        // we want to avoid routing this game entry through the URL because our current URL is very
        // nicely bookmarkable and we don't want to replace it with a non-bookmarkable URL
        _enterNextGameDirect = true;

        // the playNow() call will resolve the lobby on the game server, then attempt to start a
        // game for us; if it succeeds, it sends back a zero result and we need take no further
        // action; if it fails, it sends back the lobby OID so we can join the lobby
        lsvc.playNow(_gctx.getClient(), _gameId, mode, cb);
    }

    public function enterGame (gameOid :int) :void
    {
        _gameOid = gameOid;
        _gctx.getLocationDirector().moveTo(gameOid);

        // make a note what game we're playing, for posterity
        _ctx.getGameDirector().setMostRecentLobbyGame(_gameId);

        // clear out our lobby side panel in case it has not been cleared already
        _ctx.getTopPanel().clearLeftPanel(null);
    }

    override public function shutdown () :void
    {
        _shuttingDown = true;
        // any shutdown of the liaison kills the lobby, so check if there is one open
        if (_lobby != null) {
            _lobby.forceShutdown();
        }
        _ctx.getLocationDirector().removeLocationObserver(_worldLocObs);
        super.shutdown();
    }

    public function lobbyCleared (inGame :Boolean, closedByUser :Boolean) :void
    {
        // if we're not about to go into a game, shutdown, otherwise stick around
        if (!_shuttingDown && !inGame && _gameOid == 0) {
            shutdown();
            // we may be being closed due to navigation away from the lobby URL, so we don't want
            // to mess with the URL in that circumstance; only if the player pressed the close box
            if (closedByUser) {
                // either restore our current scene URL or go home if we have no scene
                if (_ctx.getSceneDirector().getScene() == null) {
                    _ctx.getMsoyController().handleGoScene(_ctx.getMemberObject().getHomeSceneId());
                } else {
                    _ctx.getMsoyController().restoreSceneURL();
                }
            }
        }
    }

    // from GameLiaison
    override public function clientDidLogon (event :ClientEvent) :void
    {
        super.clientDidLogon(event);
        switch (_mode) {
        case JOIN_PLAYER:
            joinPlayer(_playerIdGame);
            break;

        case PLAY_NOW_SINGLE:
        case PLAY_NOW_FRIENDS:
        case PLAY_NOW_ANYONE:
            playNow(_mode);
            break;

        default:
        case SHOW_LOBBY:
            joinLobby();
            break;
        }
    }

    // from interface GameReadyObserver
    public function receivedGameReady (gameOid :int) :Boolean
    {
        _ctx.getTopPanel().clearTableDisplay();
        if (_enterNextGameDirect) {
            _enterNextGameDirect = false;
            _ctx.getGameDirector().enterGame(gameOid);
        } else {
            _ctx.getMsoyController().handleGoGame(_gameId, gameOid);
        }
        return true;
    }

    protected function joinLobby () :void
    {
        var lsvc :LobbyService = (_gctx.getClient().requireService(LobbyService) as LobbyService);
        var cb :ResultWrapper = new ResultWrapper(function (cause :String) :void {
            _ctx.displayFeedback(MsoyCodes.GAME_MSGS, cause);
            shutdown();
        }, gotLobbyOid);
        lsvc.identifyLobby(_gctx.getClient(), _gameId, cb);
    }

    protected function worldLocationDidChange (place :PlaceObject) :void
    {
        // don't do anything if we are not yet in a game
        if (_gameOid == 0) {
            return;
        }

        if (place != null) {
            // we've left our game and returned to the world, so we want to shutdown
            shutdown();
        }
    }

    protected function gotLobbyOid (result :Object) :void
    {
        // this will create a panel and add it to the side panel on the top level
        _lobby = new LobbyController(_gctx, this, int(result), _mode);

        // if we have a player table to enter do that now
        if (_playerIdTable != 0) {
            joinPlayerTable(_playerIdTable);
        }
    }

    protected function gotPlayerGameOid (result :Object) :void
    {
        var gameOid :int = int(result);
        if (gameOid == -1) {
            // player isn't currently playing - show the lobby instead
            showLobbyUI();
            // if they're at a table, join them there
            joinPlayerTable(_playerIdGame);
        } else {
            _ctx.getMsoyController().handleGoGame(_gameId, gameOid);
        }
    }

    /** The action we'll take once we're connected to the game server. */
    protected var _mode :int;

    /** The id of the player we'd like to join. */
    protected var _playerIdGame :int = 0;

    /** The id of the player who's pending table we'd like to join. */
    protected var _playerIdTable :int = 0;

    /** Listens for world location changes. */
    protected var _worldLocObs :LocationAdapter =
        new LocationAdapter(null, worldLocationDidChange, null);

    /** Our active lobby, if we have one. */
    protected var _lobby :LobbyController;

    /** The oid of our game object, once we've been told it. */
    protected var _gameOid :int;

    /** True if we're shutting down. */
    protected var _shuttingDown :Boolean;

    /** Used to avoid routing a gameReady through the URL when we're doing playNow(). */
    protected var _enterNextGameDirect :Boolean;
}
}
