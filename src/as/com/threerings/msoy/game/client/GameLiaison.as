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
    implements MsoyGameService_LocationListener, ClientObserver
{
    public static const log :Log = Log.getLog(GameLiaison);

    public function GameLiaison (ctx :WorldContext, gameId :int)
    {
        _ctx = ctx;
        _gameId = gameId;

        // create our custom context which we'll use to connect to lobby/game servers
        _gctx = new GameContext(ctx);
        _gctx.getClient().addClientObserver(this);

        // locate the game server to start everything off
        var mgsvc :MsoyGameService =
            (_ctx.getClient().requireService(MsoyGameService) as MsoyGameService);
        mgsvc.locateGame(_ctx.getClient(), gameId, this);
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
        // subclasses can return something real here
        return null;
    }

    public function shutdown () :void
    {
        _gctx.getClient().logoff(false);
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
        // nada
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

    /** Provides access to main client services. */
    protected var _ctx :WorldContext;

    /** A separate context that connects to the game server. */
    protected var _gctx :GameContext;

    /** The id of the game with which we're dealing. */
    protected var _gameId :int;
}
}
