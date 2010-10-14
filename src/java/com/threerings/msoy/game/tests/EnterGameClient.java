//
// $Id$

package com.threerings.msoy.game.tests;

import java.util.Map;
import java.util.prefs.Preferences;

import com.google.common.collect.Maps;
import com.samskivert.util.BasicRunQueue;
import com.samskivert.util.Config;
import com.samskivert.util.RunQueue;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientAdapter;
import com.threerings.presents.dobj.DObjectManager;
import com.threerings.util.MessageManager;

import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.crowd.util.CrowdContext;
import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.client.LocationDirector;
import com.threerings.crowd.client.OccupantDirector;
import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.client.PlaceView;

import com.threerings.parlor.client.ParlorDirector;
import com.threerings.parlor.util.ParlorContext;

import com.threerings.msoy.data.MsoyAuthResponseData;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.VisitorInfo;
import com.threerings.msoy.game.client.LobbyService;
import com.threerings.msoy.game.client.WorldGameService;
import com.threerings.msoy.game.data.GameCredentials;

import static com.threerings.msoy.Log.log;

/**
 * A simple client that connects to a Whirled server and attempts to start up a single player game.
 */
public class EnterGameClient
{
    public static final int NUM_TESTERS = 100;

    public static void main (String[] args)
    {
        if (args.length < 1) {
            System.err.println("Usage: EnterGameClient [-Dserver=server -Dport=port] gameId");
            System.exit(-1);
        }

        String hostname = System.getProperty("server", "localhost");
        int port = Integer.parseInt(System.getProperty("port", ""+Client.DEFAULT_SERVER_PORTS[0]));
        int gameId = Integer.parseInt(args[0]);
        BasicRunQueue rqueue = new BasicRunQueue();
        Preferences prefs = Preferences.userNodeForPackage(EnterGameClient.class);

        // create all of our testers
        for (int ii = 0; ii < NUM_TESTERS; ii++) {
            _testers.put(ii, new Tester(prefs, rqueue, ii));
        }

        // start them all up
        for (Tester tester : _testers.values()) {
            tester.start(hostname, port, gameId);
        }

        // process the run queue which will start everything a goin'
        rqueue.run();
    }

    public static class Tester
    {
        public Tester (final Preferences prefs, RunQueue rqueue, int id)
        {
            _id = id;

            GameCredentials creds = new GameCredentials();
            creds.sessionToken = prefs.get("token." + _id, null);
            if (creds.sessionToken == null) {
                creds.visitorId = new VisitorInfo().id;
            }

            // create and configure our client
            _client = new Client(creds, rqueue);
            _client.setVersion(DeploymentConfig.version);
            _client.addServiceGroup(MsoyCodes.GAME_GROUP);
            _client.addClientObserver(new ClientAdapter() {
                public void clientDidLogon (Client client) {
                    MsoyAuthResponseData arsp = (MsoyAuthResponseData)client.getAuthResponseData();
                    prefs.put("token." + _id, arsp.sessionToken);
                }
            });

            // create our context
            _ctx = new GameContextImpl();

            // create our managers and directors
            _msgmgr = new MessageManager("rsrc.i18n");
            _locdir = new LocationDirector(_ctx) {
                protected PlaceController createController (PlaceConfig config) {
                    return new TestController();
                }
            };
            _occdir = new OccupantDirector(_ctx);
            _chatdir = new ChatDirector(_ctx, MsoyCodes.CHAT_MSGS);
            _pardtr = new ParlorDirector(_ctx);
        }

        public void start (String hostname, int port, final int gameId)
        {
            _client.setServer(hostname, new int[] { port });
            _client.addClientObserver(new ClientAdapter() {
                public void clientDidLogon (Client client) {
                    _client.removeClientObserver(this);
                    locateGame(gameId);
                }
            });
            _client.logon();
        }

        protected void locateGame (final int gameId)
        {
            // first locate the game since that's what a normal client would do
            _client.requireService(WorldGameService.class).locateGame(
                _client, gameId, new WorldGameService.LocationListener() {
                public void gameLocated (String host, int port, boolean isAVRG) {
                    log.info(_id + " locateGame -> " + host + ":" + port);
                    playGame(gameId);
                }
                public void requestFailed (String cause) {
                    log.info(_id + " locateGame failed -> " + cause);
                    shutdown();
                }
            });
        }

        protected void playGame (int gameId)
        {
            _client.requireService(LobbyService.class).playNow(
                _client, gameId, 0, new LobbyService.ResultListener() {
                public void requestProcessed (Object result) {
                    log.info(_id + " playNow -> " + result);
                }
                public void requestFailed (String cause) {
                    log.info(_id + " playNow failed -> " + cause);
                    shutdown();
                }
            });
        }

        protected void shutdown ()
        {
            _client.logoff(false);
        }

        /**
         * The context implementation. This provides access to all of the objects and services that
         * are needed by the operating client.
         */
        protected class GameContextImpl implements ParlorContext
        {
            public Client getClient () {
                return _client;
            }

            public DObjectManager getDObjectManager () {
                return _client.getDObjectManager();
            }

            public Config getConfig () {
                return _config;
            }

            public LocationDirector getLocationDirector () {
                return _locdir;
            }

            public OccupantDirector getOccupantDirector () {
                return _occdir;
            }

            public ChatDirector getChatDirector () {
                return _chatdir;
            }

            public MessageManager getMessageManager () {
                return _msgmgr;
            }

            public ParlorDirector getParlorDirector () {
                return _pardtr;
            }

            public void setPlaceView (PlaceView view) {
                // noop
            }

            public void clearPlaceView (PlaceView view) {
                // noop
            }
        }

        protected int _id;
        protected ParlorContext _ctx;
        protected Client _client;
        protected Config _config = new Config("enterGame");
        protected MessageManager _msgmgr;
        protected LocationDirector _locdir;
        protected OccupantDirector _occdir;
        protected ChatDirector _chatdir;
        protected ParlorDirector _pardtr;
    }

    protected static class TestController extends PlaceController
    {
        protected PlaceView createPlaceView (CrowdContext ctx) {
            return null;
        }
    }

    protected static Map<Integer, Tester> _testers = Maps.newHashMap();
}
