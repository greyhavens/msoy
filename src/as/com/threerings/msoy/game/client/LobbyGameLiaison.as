//
// $Id$

package com.threerings.msoy.game.client {

import com.threerings.util.Log;

import com.threerings.presents.client.ClientEvent;
import com.threerings.presents.client.ResultAdapter;

import com.threerings.crowd.client.LocationAdapter;
import com.threerings.crowd.client.LocationDirector;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.parlor.client.GameReadyObserver;

import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.world.client.WorldContext;

import com.threerings.msoy.game.data.LobbyCodes;
import com.threerings.msoy.game.data.MsoyGameConfig;
import com.threerings.presents.client.ClientAdapter;
import com.threerings.presents.data.ClientObject;

/**
 * Handles the lobby-specific aspects of the game server connection.
 */
public class LobbyGameLiaison extends GameLiaison
    implements GameReadyObserver
{
    public static const log :Log = Log.getLog(LobbyGameLiaison);

    public function LobbyGameLiaison (ctx :WorldContext, gameId :int, mode :LobbyDef,
        playerId :int = 0, token :String = "", shareMemberId :int = 0)
    {
        super(ctx, gameId);

        _mode = mode;
        _playerIdGame = playerId;

        _waitForWorldLogon = new GatedExecutor(function () :Boolean {
            return _wctx.getClient().isLoggedOn();
        });

        log.info("Started game liaison [gameId=" + _gameId + ", mode=" + _mode + "].");

        // listen for our game to be ready so that we can display it
        _gctx.getParlorDirector().addGameReadyObserver(this);

        // listen for game location changes; we don't need to clear this observer because the
        // location directory goes away when we do
        _gctx.getLocationDirector().addLocationObserver(
            new LocationAdapter(null, gameLocationDidChange, null));

        // get a notification about logins to the game server
        _wctx.getWorldClient().addClientObserver(new ClientAdapter(null, worldClientDidLogon));

        // Set the token on the game context, so it can get passed to the game.
        (_gctx as LiaisonGameContext).setShareToken(token);
        (_gctx as LiaisonGameContext).setShareMemberId(shareMemberId);

        // listen for changes in world location so that we can shutdown if we move
        var loc :LocationDirector = _wctx.getLocationDirector();
        loc.addLocationObserver(_worldLocObs);

        // create our lobby controller which will display a "locating game..." interface
        var inRoom :Boolean = (loc.getPlaceObject() != null) || loc.movePending();
        _lobby = new LobbyController(
            _gctx, _mode, lobbyCleared, playNow, lobbyLoaded, !inRoom);
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
        var cb :ResultAdapter = new ResultAdapter(gotPlayerGameOid,
            function (cause :String) :void {
                _wctx.displayFeedback(MsoyCodes.GAME_MSGS, cause);
                // some failure cases are innocuous, and should be followed up by a display of the
                // lobby; if we really are hosed, joinLobby() will cause the liaison to shut down
                _wctx.getWorldController().restoreSceneURL();
                joinLobby(LobbyDef.PLAY_NOW);
            });
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
     * Displays the lobby for the game for which we liaise, on top of the existing view,
     * in specified mode. If the lobby is already showing, this is a NOOP.
     */
    public function showLobby (mode :LobbyDef) :void
    {
        if (_lobby == null) {
            _lobby = new LobbyController(
                _gctx, mode, lobbyCleared, playNow, lobbyLoaded, false);
            joinLobby(mode);
        } // otherwise it's already showing
    }

    /**
     * Attempts to go right into a game based on the supplied mode.
     *
     * @see LobbyCodes
     */
     // TODO: this probably shouldn't be public - just use joinLobby instead
    public function playNow (mode :int) :void
    {
        var lsvc :LobbyService = (_gctx.getClient().requireService(LobbyService) as LobbyService);
        var cb :ResultAdapter = new ResultAdapter(
            function (lobbyOid :int) :void
            {
                // this is only used for testing game loading issues per WRLD-531,
                // and will be removed after the test is over. -- robert
                var stageName :String = "stage 3 " + ((lobbyOid == 0) ? "game" : "lobby");
                _wctx.getMsoyClient().trackClientAction("WRLD-531-2 game started", stageName);

                if (lobbyOid != 0) {
                    // we failed to start a game (see below) so join the lobby instead
                    gotLobbyOid(lobbyOid);
                }
            },
            function (cause :String) :void {
                _wctx.displayFeedback(MsoyCodes.GAME_MSGS, cause);
                shutdown();
            });

        // once we're logged in to the world server, start the game!
        _waitForWorldLogon.execute(function () :void {
            // the playNow() call will resolve the lobby on the game server, then attempt to start
            // a game for us; if it succeeds, it sends back a zero result and we need take no
            // further action; if it fails, it sends back the lobby OID so we can join the lobby
            lsvc.playNow(_gctx.getClient(), _gameId, mode, cb);
        });
    }

    /**
     * Opens the game's group home scene if a scene isn't already displayed.
     */
    public function lobbyLoaded (groupId :int) :void
    {
        // TODO: display loader?
    }

    /**
     * Shuts down any active lobby and enters the specified game.
     *
     * @return true if we initiated the entry request, false if we could not.
     */
    public function enterGame (gameOid :int) :Boolean
    {
        // note our game oid and enter the game location
        _gameOid = gameOid;

        // shut our lobby down now that we're entering the game
        if (_lobby != null) {
            _lobby.shutdown();
        }

        log.info("Entering game");
        if (!_gctx.getLocationDirector().moveTo(gameOid)) {
            return false;
        }

        // also leave our current world location
        if (!_wctx.getLocationDirector().leavePlace()) {
            log.warning("Uh oh, unable to leave room before entering game " +
                        "[movePending=" + _wctx.getLocationDirector().movePending() + "].");
        }

        return true;
    }

    /**
     * Leaves the currently occupied game.
     */
    public function clearGame () :void
    {
        if (_gameOid != 0) {
            _gctx.getLocationDirector().leavePlace();
            _gameOid = 0;
        }
    }

    override public function shutdown () :void
    {
        _shuttingDown = true;

        // any shutdown of the liaison kills the lobby, so check if there is one open
        if (_lobby != null) {
            _lobby.forceShutdown();
        }
        _wctx.getLocationDirector().removeLocationObserver(_worldLocObs);

        super.shutdown();
    }

    // from GameLiaison
    override public function clientDidLogon (event :ClientEvent) :void
    {
        super.clientDidLogon(event);

        // are we joining a game in progress?
        if (_playerIdGame != 0) {
            joinPlayer(_playerIdGame);
        } else {
            // just join whichever lobby we're supposed to join, however we're supposed to join
            joinLobby(_mode);
        }

        _mode = LobbyDef.LOBBY_ONLY; // in case we end up back here after the game
    }

    // from interface GameReadyObserver
    public function receivedGameReady (gameOid :int) :Boolean
    {
        _wctx.getWorldController().handleGoGame(_gameId, gameOid);

        // this is only used for testing game loading issues per WRLD-531,
        // and will be removed after the test is over. -- robert
        _wctx.getMsoyClient().trackClientAction("WRLD-531-2 game started", "stage 5");

        return true;
    }

    // from GameLiaison
    override public function get gameName () :String
    {
        var config :MsoyGameConfig = gameConfig as MsoyGameConfig;
        return (config != null) ? config.game.name : super.gameName;
    }

    // from GameLiaison
    override public function get gameGroupId () :int
    {
        var config :MsoyGameConfig = gameConfig as MsoyGameConfig;
        return (config != null) ? config.groupId : super.gameGroupId;
    }

    /** Listens for logins to the world server. */
    protected function worldClientDidLogon (event :ClientEvent) :void
    {
        _waitForWorldLogon.update();
        if (_lobby != null) {
            _lobby.worldClientDidLogon(event);
        }
    }

    protected function joinLobby (mode :LobbyDef) :void
    {
        // if we're asked to play right away, let's try to do that
        if (mode.playNow) {
            playNow(mode.playNowMode);
            return;
        }

        // otherwise just ask the server for the lobby
        var lsvc :LobbyService = (_gctx.getClient().requireService(LobbyService) as LobbyService);
        var cb :ResultAdapter = new ResultAdapter(gotLobbyOid,
            function (cause :String) :void {
                _wctx.displayFeedback(MsoyCodes.GAME_MSGS, cause);
                shutdown();
            });
        lsvc.identifyLobby(_gctx.getClient(), _gameId, cb);
    }

    protected function gameLocationDidChange (place :PlaceObject) :void
    {
        // tell the world controller to update the location display
        _wctx.getWorldController().updateLocationDisplay();
    }

    protected function worldLocationDidChange (place :PlaceObject) :void
    {
        if (_gameOid != 0 && place != null) {
            // we've left our game and returned to the world, so we want to shutdown
            log.info("Moved to a whirled room, shutting down game");
            shutdown();
        }
    }

    protected function gotLobbyOid (lobbyOid :int) :void
    {
        _lobby.enterLobby(lobbyOid);

        // if we have a player table to enter do that now
        if (_playerIdTable != 0) {
            joinPlayerTable(_playerIdTable);
            _playerIdTable = 0;
        }
    }

    protected function gotPlayerGameOid (result :Object) :void
    {
        var gameOid :int = int(result);
        if (gameOid == -1) {
            // player isn't currently playing - show the lobby instead
            joinLobby(_mode);
            // if they're at a table, join them there
            joinPlayerTable(_playerIdGame);
        } else {
            _wctx.getWorldController().handleGoGame(_gameId, gameOid);
        }
    }

    protected function lobbyCleared (closedByUser :Boolean) :void
    {
        _lobby = null;

        // if we're about to enter a game, or already shutting down, stop her
        if (_gameOid != 0 || _shuttingDown) {
            return;
        }
        // otherwise shut ourselves down
        shutdown();

        // we may be being closed due to navigation away from the lobby URL, so we don't want to
        // mess with the URL in that circumstance; only if the player pressed the close box
        if (closedByUser) {
            // if we have no scene (meaning we went right into a game and now they've canceled
            // that, close the client and take them back to the Games section
            if (_wctx.getSceneDirector().getScene() == null) {
                _wctx.getWorldClient().closeClient();
            } else {
                _wctx.getWorldController().restoreSceneURL();
            }
        }
    }

    /** Lobby action definition. */
    protected var _mode :LobbyDef;

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

    /** Executor that waits for the world client to log in. */
    protected var _waitForWorldLogon :GatedExecutor;
}
}
