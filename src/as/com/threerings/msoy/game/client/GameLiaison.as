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
 * Handles all the fiddly bits relating to connecting to a separate server to match-make and play a
 * game.
 */
public class GameLiaison
    implements MsoyGameService_LocationListener, ClientObserver, GameReadyObserver
{
    public static const log :Log = Log.getLog(GameLiaison);

    public function GameLiaison (ctx :WorldContext, gameId :int)
    {
        _ctx = ctx;
        _gameId = gameId;

        // create our custom context which we'll use to connect to lobby/game servers
        _gctx = new GameContext(ctx);
        _gctx.getClient().addClientObserver(this);
        _gctx.getParlorDirector().addGameReadyObserver(this);

        // locate the game server to start everything off
        var mgsvc :MsoyGameService =
            (_ctx.getClient().requireService(MsoyGameService) as MsoyGameService);
        mgsvc.locateGame(_ctx.getClient(), gameId, this);

        // listen for changes in world location so that we can shutdown if we move
        _ctx.getLocationDirector().addLocationObserver(_worldLocObs);
    }

    /**
     * Returns the ID of the game we're match-making for.
     */
    public function get gameId () :int
    {
        return _gameId;
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
        _lobby.restoreLobbyUI();
    }

    public function enterGame (gameOid :int) :void
    {
        _gameOid = gameOid;
        _gctx.getLocationDirector().moveTo(gameOid);

        // clear out our lobby side panel in case it has not been cleared already
        _ctx.getTopPanel().clearLeftPanel(null);
    }

    public function shutdown () :void
    {
        _ctx.getLocationDirector().removeLocationObserver(_worldLocObs);
        _gctx.getClient().logoff(false);
    }

    public function lobbyCleared (inGame :Boolean) :void
    {
        log.info("Lobby cleared [in=" + inGame + "].");

        // if we're not about to go to a game, shutdown, otherwise stick around
        if (!inGame && _gameOid == 0) {
            shutdown();
        }
    }

    // from interface MsoyGameService_LocationListener
    public function gameLocated (hostname :String, port :int) :void
    {
        log.info("Got server for " + _gameId + " [host=" + hostname + ", port=" + port + "].");
        _gctx.getClient().setServer(hostname, [ port ]);
        _gctx.getClient().setVersion(DeploymentConfig.version);
        _gctx.getClient().logon();
    }

    // from interface MsoyGameService_LocationListener
    public function requestFailed (cause :String) :void
    {
        _ctx.displayFeedback(MsoyCodes.GAME_MSGS, cause);
    }

    // from interface ClientObserver
    public function clientWillLogon (event :ClientEvent) :void
    {
        // nada
    }

    // from interface ClientObserver
    public function clientDidLogon (event :ClientEvent) :void
    {
        // join the lobby
        var lsvc :LobbyService = (_gctx.getClient().requireService(LobbyService) as LobbyService);
        var cb :ResultWrapper = new ResultWrapper(function (cause :String) :void {
            _ctx.displayFeedback(MsoyCodes.GAME_MSGS, cause);
            shutdown();
        }, gotLobbyOid);
        lsvc.identifyLobby(_gctx.getClient(), _gameId, cb);
    }

    // from interface ClientObserver
    public function clientFailedToLogon (event :ClientEvent) :void
    {
        // TODO: something fancier?
        _ctx.displayFeedback(MsoyCodes.GAME_MSGS, "e.internal_error");
        clientDidClear(null); // abandon ship
    }

    // from interface ClientObserver
    public function clientObjectDidChange (event :ClientEvent) :void
    {
        // nada
    }

    // from interface ClientObserver
    public function clientConnectionFailed (event :ClientEvent) :void
    {
        // TODO
    }

    // from interface ClientObserver
    public function clientWillLogoff (event :ClientEvent) :void
    {
        // TODO
    }

    // from interface ClientObserver
    public function clientDidLogoff (event :ClientEvent) :void
    {
        log.info("Logged off of game server [id=" + _gameId + "].");
        // TODO: anything?
    }

    // from interface ClientObserver
    public function clientDidClear (event :ClientEvent) :void
    {
        _ctx.getGameDirector().liaisonCleared(this);
    }

    // from interface GameReadyObserver
    public function receivedGameReady (gameOid :int) :Boolean
    {
        _ctx.getTopPanel().clearTableDisplay();
        _ctx.getMsoyController().handleGoGame(_gameId, gameOid);
        return true;
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
    }

    /** Provides access to main client services. */
    protected var _ctx :WorldContext;

    /** A separate context that connects to the game server. */
    protected var _gctx :GameContext;

    /** The id of the game with which we're dealing. */
    protected var _gameId :int;

    /** Listens for world location changes. */
    protected var _worldLocObs :LocationAdapter =
        new LocationAdapter(null, worldLocationDidChange, null);

    /** Our active lobby, if we have one. */
    protected var _lobby :LobbyController;

    /** The oid of our game object, once we've been told it. */
    protected var _gameOid :int;
}
}
