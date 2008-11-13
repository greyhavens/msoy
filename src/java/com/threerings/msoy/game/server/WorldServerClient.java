//
// $Id$

package com.threerings.msoy.game.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.Interval;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientAdapter;
import com.threerings.presents.client.Communicator;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.dobj.MessageEvent;
import com.threerings.presents.dobj.MessageListener;
import com.threerings.presents.dobj.RootDObjectManager;
import com.threerings.presents.peer.net.PeerCreds;
import com.threerings.presents.server.ReportManager;
import com.threerings.presents.server.ShutdownManager;
import com.threerings.presents.server.net.ConnectionManager;
import com.threerings.presents.server.net.ServerCommunicator;

import com.threerings.stats.data.IntSetStatAdder;
import com.threerings.stats.data.IntStatIncrementer;
import com.threerings.stats.data.Stat;

import com.threerings.util.Name;

import com.threerings.crowd.chat.server.ChatProvider;
import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.Prize;
import com.threerings.msoy.server.ServerConfig;

import com.threerings.msoy.data.UserAction;
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
    implements MessageListener, ShutdownManager.Shutdowner
{
    /** A notification that it is time to shut down. */
    public static final String SHUTDOWN_MESSAGE = "shutdown";

    /** A notification that game's content has been updated. */
    public static final String GAME_CONTENT_UPDATED = "gameContentUpdated";

    /** A message sent by a our world server when the player has purchased game content */
    public static final String GAME_CONTENT_PURCHASED =  "gameContentPurchased";

    /** A request that we reset our percentiler. */
    public static final String RESET_SCORE_PERCENTILER = "resetScorePercentiler";

    /** A request that we broadcast a message with our chat provider */
    public static final String FORWARD_BROADCAST = "forwardBroadcast";

    /** A request that we flush a player's coin earnings. */
    public static final String FLUSH_COIN_EARNINGS = "flushCoinEarnings";

    /**
     * Configures our listen and connection ports and connects to our parent world server.
     */
    public void init (int listenPort, int connectPort)
    {
        _port = listenPort;
        _shutMan.registerShutdowner(this);

        // create our client and connect to the server
        _client = new Client(null, _omgr) {
            protected Communicator createCommunicator () {
                return new ServerCommunicator(this, _conmgr, WorldServerClient.this._omgr);
            }
        };
        _client.setCredentials(new PeerCreds("game:" + _port, ServerConfig.sharedSecret));
        _client.setServer("localhost", new int[] { connectPort });
        _client.addServiceGroup(WorldGameRegistry.GAME_SERVER_GROUP);
        _client.addClientObserver(_clientObs);
        _watchMan.init(_client);
        _client.logon();

        // send a state of the server report to our world server every 30 seconds
        new Interval(_omgr) {
            public void expired () {
                _gssvc.reportReport(_client, _reportMan.generateReport());
            }
        }.schedule(30*1000L, true);
    }

    public void leaveAVRGame (int playerId)
    {
        if (_gssvc == null) {
            log.info("Dropping AVRGame departure [id=" + playerId + "].");
        } else {
            _gssvc.leaveAVRGame(_client, playerId);
        }
    }

    public void updatePlayer (int playerId, Game game)
    {
        if (_gssvc == null) {
            log.info("Dropping update notification [id=" + playerId + ", game=" + game + "].");
        } else {
            _gssvc.updatePlayer(_client, playerId, game == null ? null : new GameSummary(game));
        }
    }

    public void stoppedHostingGame (int gameId)
    {
        log.info("Stopped hosting " + gameId + ".");
        if (_gssvc == null) {
            log.info("Dropping unhosting notification " + gameId + ".");
        } else {
            _gssvc.clearGameHost(_client, _port, gameId);
        }
    }

    public void reportCoinAward (int memberId, int deltaCoins)
    {
        if (_gssvc == null) {
            log.info("Dropping flow award [mid=" + memberId + ", df=" + deltaCoins + "].");
        } else {
            _gssvc.reportCoinAward(_client, memberId, deltaCoins);
        }
    }

    public void awardCoins (int gameId, UserAction action, int amount)
    {
        if (_gssvc == null) {
            log.warning("Dropping coin award", "gameId", gameId, "action", action, "amount", amount);
        } else {
            _gssvc.awardCoins(_client, gameId, action, amount);
        }
    }

    public void reportTrophyAward (int memberId, String gameName, Trophy trophy)
    {
        if (_gssvc == null) {
            log.info("Dropping trophy award [mid=" + memberId + ", trophy=" + trophy + "].");
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
            log.info("Dropping incrementStat", "Stat.Type", type, "delta", delta);
        } else {
            _gssvc.updateStat(_client, memberId, new IntStatIncrementer(type, delta));
        }
    }

    public void addToSetStat (int memberId, Stat.Type type, int value)
    {
        if (_gssvc == null) {
            log.info("Dropping addToSetStat", "Stat.Type", type, "value", value);
        } else {
            _gssvc.updateStat(_client, memberId, new IntSetStatAdder(type, value));
        }
    }
    
    public void notifyMemberStartedGame (int memberId, byte action, int gameId) 
    {
        if (_gssvc == null) {
            log.info("Dropping addExperience", "action", action, "gameId", gameId);
        } else {
            _gssvc.notifyMemberStartedGame(_client, memberId, action, gameId);
        }
    }

    // from interface MessageListener
    public void messageReceived (MessageEvent event)
    {
        if (event.getName().equals(SHUTDOWN_MESSAGE)) {
            // TODO: we could just stop listening for client connections and shut ourselves down
            // once all the games on this server have finally ended; might be fiddly
            log.info("Got shutdown notification from world server.");
            _shutMan.shutdown();

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

        } else if (event.getName().equals(FLUSH_COIN_EARNINGS)) {
            _gameReg.flushCoinEarnings((Integer)event.getArgs()[0]);

        } else if (event.getName().equals(GAME_CONTENT_PURCHASED)) {
            Object[] args = event.getArgs();
            int playerId = (Integer) args[0];
            int gameId = (Integer) args[1];
            byte itemType = (Byte) args[2];
            String ident = (String) args[3];
            _gameReg.gameContentPurchased(playerId, gameId, itemType, ident);
        }
    }

    // from interface ShutdownManager.Shutdowner
    public void shutdown ()
    {
        // logoff if we're shutting down
        if (_client.isLoggedOn()) {
            _client.logoff(false);
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
            _shutMan.shutdown(); // TODO: see SHUTDOWN_MESSAGE handler
        }
    };

    protected int _port;
    protected Client _client;
    protected GameServerService _gssvc;

    @Inject protected ConnectionManager _conmgr;
    @Inject protected RootDObjectManager _omgr;
    @Inject protected ShutdownManager _shutMan;
    @Inject protected ReportManager _reportMan;
    @Inject protected GameGameRegistry _gameReg;
    @Inject protected GameWatcherManager _watchMan;
    @Inject protected ChatProvider _chatProv;
}
