//
// $Id$

package com.threerings.msoy.game.server;

import java.util.logging.Level;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientAdapter;
import com.threerings.presents.dobj.MessageEvent;
import com.threerings.presents.dobj.MessageListener;
import com.threerings.presents.peer.net.PeerCreds;

import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.server.ServerConfig;

import com.threerings.msoy.game.client.GameServerService;
import com.threerings.msoy.game.data.GameSummary;

import static com.threerings.msoy.Log.log;

/**
 * Connects back to our parent world server and keeps it up to date as to our goings on.
 */
public class WorldServerClient
    implements MessageListener
{
    /** A message sent by our world server to let us know to shut down. */
    public static final String SHUTDOWN_MESSAGE = "shutdown";

    public void init (MsoyGameServer server, int port)
    {
        _server = server;
        _port = port;

        // create our client and connect to the server
        _client = new Client(null, MsoyGameServer.omgr);
        _client.setCredentials(new PeerCreds("game:" + port, ServerConfig.sharedSecret));
        _client.setServer("localhost", ServerConfig.serverPorts);
        _client.addServiceGroup(MsoyGameRegistry.GAME_SERVER_GROUP);
        _client.addClientObserver(_clientObs);
        _client.logon();
    }

    public void updatePlayer (int playerId, Game game)
    {
        if (_gssvc == null) {
            log.warning("Dropping update notification [id=" + playerId + ", game=" + game + "].");
        } else if (playerId > 0) { // don't update guests
            _gssvc.updatePlayer(_client, playerId, game == null ? null : new GameSummary(game));
        }
    }

    public void stoppedHostingGame (int gameId)
    {
        if (_gssvc == null) {
            log.warning("Dropping unhosting notification " + gameId + ".");
        } else {
            _gssvc.clearGameHost(_client, _port, gameId);
        }
    }

    public void reportFlowAward (int memberId, int deltaFlow)
    {
        if (_gssvc == null) {
            log.warning("Dropping flow award [mid=" + memberId + ", df=" + deltaFlow + "].");
        } else {
            _gssvc.reportFlowAward(_client, memberId, deltaFlow);
        }
    }

    // from interface MessageListener
    public void messageReceived (MessageEvent event)
    {
        if (event.getName().equals(SHUTDOWN_MESSAGE)) {
            // TODO: we could just stop listening for client connections and shut ourselves down
            // once all the games on this server have finally ended; might be fiddly
            log.info("Got shutdown notification from world server.");
            _server.shutdown();
        }
    }

    protected ClientAdapter _clientObs = new ClientAdapter() {
        public void clientFailedToLogon (Client client, Exception cause) {
            log.log(Level.WARNING, "Failed to connect to world server.", cause);
        }

        public void clientDidLogon (Client client) {
            log.info("Connected to world server.");
            _gssvc = (GameServerService)_client.requireService(GameServerService.class);
            _gssvc.sayHello(client, _port);
            client.getClientObject().addListener(WorldServerClient.this);
        }

        public void clientDidLogoff (Client client) {
            log.info("Logged off of world server.");
            _gssvc = null;
            _server.shutdown(); // TODO: see SHUTDOWN_MESSAGE handler
        }
    };

    protected MsoyGameServer _server;
    protected int _port;
    protected Client _client;
    protected GameServerService _gssvc;
}
