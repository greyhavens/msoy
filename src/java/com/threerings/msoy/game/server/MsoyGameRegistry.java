//
// $Id$

package com.threerings.msoy.game.server;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.Tuple;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;

import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.ServerConfig;

import com.threerings.msoy.game.client.MsoyGameService;

import static com.threerings.msoy.Log.log;

/**
 * Manages the process of starting up external game server processes and coordinating with them as
 * they host lobbies and games.
 */
public class MsoyGameRegistry
    implements MsoyGameProvider
{
    /**
     * Initializes this registry.
     */
    public void init (InvocationManager invmgr)
    {
        invmgr.registerDispatcher(new MsoyGameDispatcher(this), MsoyCodes.GAME_GROUP);
    }

    // from interface MsoyGameProvider
    public void locateGame (ClientObject caller, int gameId,
                            MsoyGameService.LocationListener listener)
        throws InvocationException
    {
        // if we're already hosting this game, then report back immediately
        GameHandler handler = _handlers.get(gameId);
        if (handler != null) {
            listener.gameLocated(ServerConfig.serverHost, handler.port);
            return;
        }

        // otherwise check to see if someone else is hosting this game
        Tuple<String, Integer> rhost = MsoyServer.peerMan.getGameHost(gameId);
        if (rhost != null) {
            sendToNode(rhost.left, rhost.right, listener);
            return;
        }

        // otherwise obtain a lock and resolve the game ourselves
        // TODO
    }

    protected void sendToNode (String nodename, int port, MsoyGameService.LocationListener listener)
    {
        String hostname = MsoyServer.peerMan.getPeerPublicHostName(nodename);
        log.info("Sending game player to " + hostname + ":" + port + ".");
        listener.gameLocated(hostname, port);
    }

    /** Manages a game server process. */
    protected class GameHandler
    {
        /** The port on which this game server is listening for connections. */
        public int port;
    }

    /** Contains a mapping from gameId to handler for all game servers hosted on this machine. */
    protected HashIntMap<GameHandler> _handlers = new HashIntMap<GameHandler>();
}
