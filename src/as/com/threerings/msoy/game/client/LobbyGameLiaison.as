//
// $Id$

package com.threerings.msoy.game.client {

import com.threerings.util.Log;

import com.threerings.presents.client.ClientEvent;
import com.threerings.presents.client.ResultAdapter;

import com.threerings.crowd.client.LocationAdapter;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.parlor.client.GameReadyObserver;

import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.world.client.WorldContext;

import com.threerings.msoy.game.data.LobbyCodes;
import com.threerings.msoy.game.data.MsoyGameConfig;

/**
 * Handles the lobby-specific aspects of the game server connection.
 */
public class LobbyGameLiaison extends GameLiaison
    implements GameReadyObserver
{
    public static const log :Log = Log.getLog(LobbyGameLiaison);

    public function LobbyGameLiaison (ctx :WorldContext, gameId :int, mode :int, playerId :int = 0)
    {
        super(ctx, gameId);

        _mode = mode;
        _playerIdGame = playerId;

        log.info("Started game liaison [gameId=" + _gameId + ", mode=" + _mode + "].");

        // listen for our game to be ready so that we can display it
        _gctx.getParlorDirector().addGameReadyObserver(this);

        // listen for game location changes; we don't need to clear this observer because the
        // location directory goes away when we do
        _gctx.getLocationDirector().addLocationObserver(
            new LocationAdapter(null, gameLocationDidChange, null));

        // listen for changes in world location so that we can shutdown if we move
        _wctx.getLocationDirector().addLocationObserver(_worldLocObs);

        // create our lobby controller which will display a "locating game..." interface
        _lobby = new LobbyController(_gctx, _mode, lobbyCleared, playNow, lobbyLoaded);
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
                joinLobby();
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
     * Displays the lobby for the game for which we liaise. If the lobby is already showing, this
     * is a NOOP.
     */
    public function showLobby () :void
    {
        if (_lobby == null) {
            _lobby = new LobbyController(
                _gctx, _mode = LobbyCodes.SHOW_LOBBY, lobbyCleared, playNow, lobbyLoaded);
            joinLobby();
        } // otherwise it's already showing
    }

    /**
     * Attempts to go right into a game based on the supplied mode.
     *
     * @see LobbyCodes
     */
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

        // the playNow() call will resolve the lobby on the game server, then attempt to start a
        // game for us; if it succeeds, it sends back a zero result and we need take no further
        // action; if it fails, it sends back the lobby OID so we can join the lobby
        lsvc.playNow(_gctx.getClient(), _gameId, mode, cb);
    }

    /**
     * Opens the game's group home scene if a scene isn't already displayed.
     */
    public function lobbyLoaded (groupId :int) :void
    {
        // TODO: check that we're not already where we're about to go
        if (groupId > 0 && _wctx.getWorldController().getCurrentSceneId() == 0) {
            _goingToGroupHome = true;
            _wctx.getWorldController().handleGoGroupHome(groupId);
        }
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

        // if we've already started going to the group home, wait until we get there
        if (_goingToGroupHome) {
            return true;
        }

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
        switch (_mode) {
        case LobbyCodes.JOIN_PLAYER:
            joinPlayer(_playerIdGame);
            _mode = LobbyCodes.SHOW_LOBBY; // in case we end up back here after the game
            break;

        case LobbyCodes.PLAY_NOW_SINGLE:
        case LobbyCodes.PLAY_NOW_ANYONE:
            playNow(_mode);
            _mode = LobbyCodes.SHOW_LOBBY; // in case we end up back here after the game
            break;

        default:
        case LobbyCodes.SHOW_LOBBY:
            joinLobby();
            break;
        }
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

    override public function clientDidLogoff (event :ClientEvent) :void
    {
        super.clientDidLogoff();
        _goingToGroupHome = false;
    }

    protected function joinLobby () :void
    {
//        var lsvc :LobbyService = (_gctx.getClient().requireService(LobbyService) as LobbyService);
//        var cb :ResultAdapter = new ResultAdapter(gotLobbyOid,
//            function (cause :String) :void {
//                _wctx.displayFeedback(MsoyCodes.GAME_MSGS, cause);
//                shutdown();
//            });
//        lsvc.identifyLobby(_gctx.getClient(), _gameId, cb);

        // It's the new-style! Same as above, but if single player is the only option-
        // go right in
        playNow(LobbyCodes.PLAY_NOW_IF_SINGLE);
    }

    protected function gameLocationDidChange (place :PlaceObject) :void
    {
        // tell the world controller to update the location display
        _wctx.getWorldController().updateLocationDisplay();
    }

    protected function worldLocationDidChange (place :PlaceObject) :void
    {
        // don't do anything if we are not yet in a game
        if (_gameOid == 0) {
            return;
        }

        if (_goingToGroupHome) {
            // assume this is a result of our handleGoGroupHome
            _goingToGroupHome = false;
            enterGame(_gameOid);
            
        } else if (place != null) {
            // we've left our game and returned to the world, so we want to shutdown
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
            joinLobby();
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

    /** Whether we are waiting on the scene/location directors to get us to the game hall. */
    protected var _goingToGroupHome :Boolean;

    /** True if we're shutting down. */
    protected var _shuttingDown :Boolean;
}
}
