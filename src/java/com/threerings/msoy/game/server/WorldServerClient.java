//
// $Id$

package com.threerings.msoy.game.server;

import java.util.logging.Level;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientAdapter;
import com.threerings.presents.peer.net.PeerCreds;

import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.server.ServerConfig;

import com.threerings.msoy.game.client.GameServerService;

import static com.threerings.msoy.Log.log;

/**
 * Connects back to our parent world server and keeps it up to date as to our goings on.
 */
public class WorldServerClient
{
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

    public void updateHostedGame (int gameId, int players)
    {
        if (_gssvc == null) {
            log.warning("Dropping update notification [id=" + gameId + ", pl=" + players + "].");
        } else {
            _gssvc.updateGameInfo(_client, gameId, players);
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

    protected ClientAdapter _clientObs = new ClientAdapter() {
        public void clientFailedToLogon (Client client, Exception cause) {
            log.log(Level.WARNING, "Failed to connect to world server.", cause);
        }

        public void clientDidLogon (Client client) {
            log.info("Connected to world server.");
            _gssvc = (GameServerService)_client.requireService(GameServerService.class);
        }

        public void clientDidLogoff (Client client) {
            log.info("Logged off of world server.");
            _gssvc = null;
            // TEMP: for now shut ourselves down
            _server.shutdown();
        }
    };

    protected MsoyGameServer _server;
    protected int _port;
    protected Client _client;
    protected GameServerService _gssvc;
}
