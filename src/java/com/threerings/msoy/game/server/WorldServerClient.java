//
// $Id$

package com.threerings.msoy.game.server;

import com.threerings.presents.client.BlockingCommunicator;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientAdapter;
import com.threerings.presents.client.Communicator;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.dobj.MessageEvent;
import com.threerings.presents.dobj.MessageListener;
import com.threerings.presents.peer.net.PeerCreds;

import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.Prize;
import com.threerings.msoy.server.ServerConfig;

import com.threerings.msoy.game.client.GameServerService;
import com.threerings.msoy.game.data.GameSummary;
import com.threerings.msoy.game.data.all.Trophy;

import static com.threerings.msoy.Log.log;

/**
 * Connects back to our parent world server and keeps it up to date as to our goings on.
 */
public class WorldServerClient
    implements MessageListener
{
    /** A message sent by our world server to let us know to shut down. */
    public static final String SHUTDOWN_MESSAGE = "shutdown";

    /** A message sent by our world server to let us know a game record has been updated. */
    public static final String GAME_RECORD_UPDATED = "gameRecordUpdated";

    /** A message sent by our world server to request that we reset our percentiler. */
    public static final String RESET_SCORE_PERCENTILER = "resetScorePercentiler";

    public void init (MsoyGameServer server, int listenPort, int connectPort)
    {
        _server = server;
        _port = listenPort;

        // create our client and connect to the server
        _client = new Client(null, MsoyGameServer.omgr) {
            protected Communicator createCommunicator () {
                // TODO: make a custom communicator that uses the ClientManager NIO system to do
                // its I/O instead of using two threads and blocking socket I/O
                return new BlockingCommunicator(this);
            }
        };
        _client.setCredentials(new PeerCreds("game:" + _port, ServerConfig.sharedSecret));
        _client.setServer("localhost", new int[] { connectPort });
        _client.addServiceGroup(MsoyGameRegistry.GAME_SERVER_GROUP);
        _client.addClientObserver(_clientObs);
        _client.logon();
    }

    public void leaveAVRGame (int playerId)
    {
        if (_gssvc == null) {
            log.warning("Dropping AVRGame departure [id=" + playerId + "].");
        } else {
            _gssvc.leaveAVRGame(_client, playerId);
        }
    }

    public void updatePlayer (int playerId, Game game)
    {
        if (_gssvc == null) {
            log.warning("Dropping update notification [id=" + playerId + ", game=" + game + "].");
        } else {
            _gssvc.updatePlayer(_client, playerId, game == null ? null : new GameSummary(game));
        }
    }

    public void stoppedHostingGame (int gameId)
    {
        log.info("Stopped hosting " + gameId + ".");
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

    public void reportTrophyAward (int memberId, String gameName, Trophy trophy)
    {
        if (_gssvc == null) {
            log.warning("Dropping trophy award [mid=" + memberId + ", trophy=" + trophy + "].");
        } else {
            _gssvc.reportTrophyAward(_client, memberId, gameName, trophy);
        }
    }

    public void awardPrize (int memberId, int gameId, String gameName, Prize prize,
                            InvocationService.ResultListener listener)
    {
        if (_gssvc == null) {
            log.warning("Dropping prize award [mid=" + memberId + ", prize=" + prize + "].");
        } else {
            _gssvc.awardPrize(_client, memberId, gameId, gameName, prize, listener);
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

        } else if (event.getName().equals(GAME_RECORD_UPDATED)) {
            int gameId = (Integer)event.getArgs()[0];
            MsoyGameServer.gameReg.gameRecordUpdated(gameId);

        } else if (event.getName().equals(RESET_SCORE_PERCENTILER)) {
            int gameId = (Integer)event.getArgs()[0];
            boolean single = (Boolean)event.getArgs()[1];
            MsoyGameServer.gameReg.resetScorePercentiler(gameId, single);
        }
    }

    protected ClientAdapter _clientObs = new ClientAdapter() {
        public void clientFailedToLogon (Client client, Exception cause) {
            log.warning("Failed to connect to world server.", cause);
        }

        public void clientDidLogon (Client client) {
            log.info("Connected to world server.");
            _gssvc = _client.requireService(GameServerService.class);
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
