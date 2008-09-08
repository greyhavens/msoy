//
// $Id$

package com.threerings.msoy.game.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.threerings.presents.client.BlockingCommunicator;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientAdapter;
import com.threerings.presents.client.Communicator;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.dobj.MessageEvent;
import com.threerings.presents.dobj.MessageListener;
import com.threerings.presents.dobj.RootDObjectManager;
import com.threerings.presents.peer.net.PeerCreds;
import com.threerings.presents.server.ShutdownManager;

import com.threerings.stats.data.IntSetStatAdder;
import com.threerings.stats.data.IntStatIncrementer;
import com.threerings.stats.data.Stat;

import com.threerings.util.Name;

import com.threerings.crowd.chat.server.ChatProvider;
import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.Prize;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.world.client.WatcherDecoder;
import com.threerings.msoy.world.client.WatcherReceiver;
import com.threerings.msoy.world.client.WatcherService;

import com.threerings.msoy.game.client.GameServerService;
import com.threerings.msoy.game.data.GameSummary;
import com.threerings.msoy.game.server.GameGameRegistry;
import com.threerings.msoy.game.data.all.Trophy;


import static com.threerings.msoy.Log.log;

/**
 * Connects back to our parent world server and keeps it up to date as to our goings on.
 */
@Singleton
public class WorldServerClient
    implements MessageListener
{
    /** A message sent by our world server to let us know to shut down. */
    public static final String SHUTDOWN_MESSAGE = "shutdown";

    /** A message sent by our world server to let us know a game's content has been updated. */
    public static final String GAME_CONTENT_UPDATED = "gameContentUpdated";

    /** A message sent by our world server to request that we reset our percentiler. */
    public static final String RESET_SCORE_PERCENTILER = "resetScorePercentiler";

    /** A message sent by our world server to request that we broadcast a message with our
     * chat provider */
    public static final String FORWARD_BROADCAST = "forwardBroadcast";

    /**
     * Configures our listen and connection ports and connects to our parent world server.
     */
    public void init (int listenPort, int connectPort)
    {
        _port = listenPort;

        // create our client and connect to the server
        _client = new Client(null, _omgr) {
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
        _client.getInvocationDirector().registerReceiver(new WatcherDecoder(_watchRec));
        _client.logon();
    }

    public void addWatch (int playerId)
    {
        if (_wsvc == null) {
            log.warning("Dropping watch request [id=" + playerId + "]");
        } else {
            _wsvc.addWatch(_client, playerId);
        }
    }

    public void clearWatch (int playerId)
    {
        if (_wsvc == null) {
            log.warning("Dropping watch clear request [id=" + playerId + "]");
        } else {
            _wsvc.clearWatch(_client, playerId);
        }
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

    public void incrementStat (int memberId, Stat.Type type, int delta)
    {
        if (_gssvc == null) {
            log.warning("Dropping incrementStat", "Stat.Type", type, "delta", delta);
        } else {
            _gssvc.updateStat(_client, memberId, new IntStatIncrementer(type, delta));
        }
    }

    public void addToSetStat (int memberId, Stat.Type type, int value)
    {
        if (_gssvc == null) {
            log.warning("Dropping addToSetStat", "Stat.Type", type, "value", value);
        } else {
            _gssvc.updateStat(_client, memberId, new IntSetStatAdder(type, value));
        }
    }

    // from interface MessageListener
    public void messageReceived (MessageEvent event)
    {
        if (event.getName().equals(SHUTDOWN_MESSAGE)) {
            // TODO: we could just stop listening for client connections and shut ourselves down
            // once all the games on this server have finally ended; might be fiddly
            log.info("Got shutdown notification from world server.");
            _shutmgr.shutdown();

        } else if (event.getName().equals(GAME_CONTENT_UPDATED)) {
            int gameId = (Integer)event.getArgs()[0];
            _gameReg.gameContentUpdated(gameId);

        } else if (event.getName().equals(RESET_SCORE_PERCENTILER)) {
            int gameId = (Integer)event.getArgs()[0];
            boolean single = (Boolean)event.getArgs()[1];
            _gameReg.resetScorePercentiler(gameId, single);

        } else if (event.getName().equals(FORWARD_BROADCAST)) {
            Object[] args = event.getArgs();
            Name sender = (Name)args[0];
            String bundle = (String)args[1];
            String msg = (String)args[2];
            boolean attention = (Boolean)args[3];
            _chatProv.broadcast(sender, bundle, msg, attention, false);
        }
    }

    protected WatcherReceiver _watchRec = new WatcherReceiver() {
        public void memberMoved (int memberId, int sceneId, String hostname, int port) {
            _watchmgr.memberMoved(memberId, sceneId, hostname, port);
        }
    };

    protected ClientAdapter _clientObs = new ClientAdapter() {
        public void clientFailedToLogon (Client client, Exception cause) {
            log.warning("Failed to connect to world server.", cause);
        }

        public void clientDidLogon (Client client) {
            log.info("Connected to world server.");
            _gssvc = _client.requireService(GameServerService.class);
            _gssvc.sayHello(client, _port);
            client.getClientObject().addListener(WorldServerClient.this);

            _wsvc = _client.requireService(WatcherService.class);
        }

        public void clientDidLogoff (Client client) {
            log.info("Logged off of world server.");
            _gssvc = null;
            _wsvc = null;
            _shutmgr.shutdown(); // TODO: see SHUTDOWN_MESSAGE handler
        }
    };

    protected int _port;
    protected Client _client;
    protected GameServerService _gssvc;
    protected WatcherService _wsvc;

    @Inject protected ShutdownManager _shutmgr;
    @Inject protected GameGameRegistry _gameReg;
    @Inject protected GameWatcherManager _watchmgr;
    @Inject protected RootDObjectManager _omgr;
    @Inject protected ChatProvider _chatProv;
}
