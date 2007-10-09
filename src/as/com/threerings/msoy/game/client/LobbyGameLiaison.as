//
// $Id$

package com.threerings.msoy.game.client {

import com.threerings.presents.client.ClientEvent;
import com.threerings.presents.client.ClientObserver;
import com.threerings.presents.client.ResultWrapper;

import com.threerings.crowd.client.LocationAdapter;
import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.parlor.client.GameReadyObserver;

import com.threerings.msoy.client.DeploymentConfig;
import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.game.data.MsoyGameConfig;

/**
 * Handles the lobby-specific aspects of the game server connection.
 */
public class LobbyGameLiaison extends GameLiaison
    implements GameReadyObserver
{
    public static const log :Log = Log.getLog(LobbyGameLiaison);

    public function LobbyGameLiaison (ctx :WorldContext, gameId :int, playerId :int = 0)
    {
        super(ctx, gameId);

        _playerIdGame = playerId;

        _gctx.getParlorDirector().addGameReadyObserver(this);

        // listen for changes in world location so that we can shutdown if we move
        _ctx.getLocationDirector().addLocationObserver(_worldLocObs);
    }

    /**
     * Join the player in their running game if possible, otherwise simply display the game
     * lobby, if it isn't up already. 
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
            return;
        }
        _lobby.joinPlayerTable(playerId);
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

    public function enterGame (gameOid :int) :void
    {
        _gameOid = gameOid;
        _gctx.getLocationDirector().moveTo(gameOid);

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

    public function lobbyCleared (inGame :Boolean) :void
    {
        // if we're not about tabout to go to a game, shutdown, otherwise stick around
        if (!_shuttingDown && !inGame && _gameOid == 0) {
            shutdown();
        }
    }

    // from GameLiaison
    override public function clientDidLogon (event :ClientEvent) :void
    {
        super.clientDidLogon(event);
        if (_playerIdGame != 0) {
            joinPlayer(_playerIdGame);
        } else {
            joinLobby();
        }
    }

    // from interface GameReadyObserver
    public function receivedGameReady (gameOid :int) :Boolean
    {
        _ctx.getTopPanel().clearTableDisplay();
        _ctx.getMsoyController().handleGoGame(_gameId, gameOid);
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
        _lobby = new LobbyController(_ctx, _gctx, this, int(result));
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
        } else {
            _ctx.getMsoyController().handleGoGame(_gameId, gameOid);
        }
    }

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
}
}
