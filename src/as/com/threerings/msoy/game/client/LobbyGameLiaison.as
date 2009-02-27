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

import com.threerings.msoy.game.data.MsoyGameConfig;
import com.threerings.presents.client.ClientAdapter;

/**
 * Handles the lobby-specific aspects of the game server connection.
 */
public class LobbyGameLiaison extends GameLiaison
    implements GameReadyObserver
{
    public static const log :Log = Log.getLog(LobbyGameLiaison);

    public function LobbyGameLiaison (ctx :WorldContext, gameId :int)
    {
        super(ctx, gameId);

        // listen for our game to be ready so that we can display it
        _gctx.getParlorDirector().addGameReadyObserver(this);

        // listen for game location changes; we don't need to clear this observer because the
        // location directory goes away when we do
        _gctx.getLocationDirector().addLocationObserver(
            new LocationAdapter(null, gameLocationDidChange, null));

        // get a notification about logins to the world server
        _wctx.getWorldClient().addClientObserver(new ClientAdapter(null, worldClientDidLogon));

        // listen for changes in world location so that we can shutdown if we move
        _worldLocObs = new LocationAdapter(null, worldLocationDidChange, null);
        _wctx.getLocationDirector().addLocationObserver(_worldLocObs);
    }

    /**
     * Displays the lobby for the game for which we liaise, on top of the existing view. If the
     * lobby is already showing, this is a NOOP.
     */
    public function showLobby () :void
    {
        if (_lobby != null) {
            return; // it's already showing
        }
        withLobbyService(function (lsvc :LobbyService) :void {
            lsvc.identifyLobby(
                _gctx.getClient(), _gameId, new ResultAdapter(gotLobbyOid, onFailure))
        });
    }

    /**
     * Requests to play the specified game.
     */
    public function playNow (playerId :int) :void
    {
        withLobbyService(function (lsvc :LobbyService) :void {
            // the playNow() call will attempt to send us into the appropriate game (that of our
            // requested player or one for ourselves); if it succeeds, it sends back a zero result
            // and we need take no further action; if it fails, it sends back the lobby oid so that
            // we can display the lobby
            lsvc.playNow(_gctx.getClient(), _gameId, playerId, new ResultAdapter(
                function (lobbyOid :int) :void {
                    if (lobbyOid != 0) {
                        gotLobbyOid(lobbyOid, playerId);
                    }
                }, onFailure));
        });
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

    // from interface GameReadyObserver
    public function receivedGameReady (gameOid :int) :Boolean
    {
        _wctx.getGameDirector().dispatchGameReady(_gameId, gameOid);
        return true;
    }

    // from GameLiaison
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

        // if we have a pending action, do that now
        if (_onLogon != null) {
            withLobbyService(_onLogon);
            _onLogon = null;
        }
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
        if (_lobby != null) {
            _lobby.worldClientDidLogon(event);
        }
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

    protected function withLobbyService (action :Function) :void
    {
        if (_gctx.getClient().isLoggedOn()) {
            action(_gctx.getClient().requireService(LobbyService) as LobbyService);
        } else {
            _onLogon = action;
        }
    }

    protected function gotLobbyOid (lobbyOid :int, playerId :int = 0) :void
    {
        // if we have some old lobby for any reason, nix it
        if (_lobby != null) {
            _lobby.shutdown();
        }

        // create our lobby controller which will create and open the lobby UI
        var wloc :LocationDirector = _wctx.getLocationDirector();
        var inRoom :Boolean = (wloc.getPlaceObject() != null) || wloc.movePending();
        var gloc :LocationDirector = _gctx.getLocationDirector();
        var inGame :Boolean = (gloc.getPlaceObject() != null) || gloc.movePending();
        _lobby = new LobbyController(_gctx, lobbyOid, lobbyCleared, !inRoom && !inGame);
        if (playerId != 0) {
            _lobby.joinPlayerTable(playerId);
        }
    }

    protected function onFailure (cause :String) :void
    {
        _wctx.displayFeedback(MsoyCodes.GAME_MSGS, cause);
        shutdown();
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

    /** Something to do once we're logged on. */
    protected var _onLogon :Function;

    /** Listens for world location changes. */
    protected var _worldLocObs :LocationAdapter;

    /** Our active lobby, if we have one. */
    protected var _lobby :LobbyController;

    /** The oid of our game object, once we've been told it. */
    protected var _gameOid :int;

    /** True if we're shutting down. */
    protected var _shuttingDown :Boolean;
}
}
