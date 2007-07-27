//
// $Id$

package com.threerings.msoy.game.server;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;

import com.samskivert.jdbc.StaticConnectionProvider;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.util.LoggingLogProvider;
import com.samskivert.util.OneLineLogFormatter;

import com.threerings.util.Name;

import com.threerings.presents.dobj.RootDObjectManager;
import com.threerings.presents.net.AuthRequest;
import com.threerings.presents.server.Authenticator;
import com.threerings.presents.server.ClientResolver;
import com.threerings.presents.server.InvocationManager;
import com.threerings.presents.server.PresentsClient;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.server.CrowdServer;
import com.threerings.crowd.server.PlaceManager;
import com.threerings.crowd.server.PlaceRegistry;

import com.threerings.ezgame.server.DictionaryManager;
import com.threerings.ezgame.server.persist.GameCookieRepository;
import com.threerings.stats.server.persist.StatRepository;

import com.threerings.parlor.game.server.GameManager;
import com.threerings.parlor.rating.server.persist.RatingRepository;
import com.threerings.parlor.server.ParlorManager;

import com.threerings.msoy.item.server.persist.GameRepository;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.MsoyObjectAccess;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.persist.MemberRepository;

import static com.threerings.msoy.Log.log;

/**
 * A bare bones server that does nothing but host a particular game.
 */
public class MsoyGameServer extends CrowdServer
{
    /** Provides database access to all of our repositories. */
    public static PersistenceContext perCtx;

    /** Contains information on our members. */
    public static MemberRepository memberRepo;

    /** Contains information on our games. */
    public static GameRepository gameRepo;

    /** Contains the rating data for each player and game. */
    public static RatingRepository ratingRepo;

//     /** Maintains "smart" digital item memories. */
//     public static MemoryRepository memoryRepo;

    /** Manages the persistent repository of stats. */
    public static StatRepository statRepo;

    /** Stores user's game cookies. */
    public static GameCookieRepository gameCookieRepo;

//     /** The Msoy item manager. */
//     public static ItemManager itemMan = new ItemManager();

    /** The parlor manager in operation on this server. */
    public static ParlorManager parlorMan = new ParlorManager();

    /** The lobby registry for this server. */
    public static LobbyRegistry lobbyReg = new LobbyRegistry();

    /** Handles sandboxed game server code. */
    public static HostedGameManager hostedMan = new HostedGameManager();

    /**
     * Called when a member starts their session to associate the name with the member's
     * distributed object.
     */
    public static void memberLoggedOn (MemberObject memobj)
    {
        _online.put(memobj.memberName, memobj);
    }

    /**
     * Called when a member ends their session to clear their name to member object mapping.
     */
    public static void memberLoggedOff (MemberObject memobj)
    {
        _online.remove(memobj.memberName);
    }

    /**
     * Starts everything a runnin'.
     */
    public static void main (String[] args)
    {
        // set up the proper logging services
        com.samskivert.util.Log.setLogProvider(new LoggingLogProvider());
        OneLineLogFormatter.configureDefaultHandler();

        // TODO: get info from command line arguments
        MsoyGameServer server = new MsoyGameServer();
        try {
            server.init();
            server.run();
        } catch (Exception e) {
            log.log(Level.WARNING, "Unable to initialize server", e);
            System.exit(255);
        }
    }

    @Override
    public void init ()
        throws Exception
    {
        // create our connection provider before calling super.init() because our superclass will
        // attempt to create our authenticator and we need the connection provider ready by then
        StaticConnectionProvider conProv =
            new StaticConnectionProvider(ServerConfig.getJDBCConfig());

        // see if we have a cache adapter for Depot
        perCtx = new PersistenceContext("msoy", conProv, ServerConfig.createCacheAdapter());

        // this is needed in createAuthenticator() so we create it early
        memberRepo = new MemberRepository(perCtx);

        super.init();

//         // set up the right client factory
//         clmgr.setClientFactory(new ClientFactory() {
//             public PresentsClient createClient (AuthRequest areq) {
//                 return new MsoyGameClient();
//             }
//             public ClientResolver createClientResolver (Name username) {
//                 return new MsoyGameClientResolver();
//             }
//         });

        // set up our default object access controller
        omgr.setDefaultAccessController(MsoyObjectAccess.DEFAULT);

        // create our various repositories
        gameRepo = new GameRepository(perCtx);
        ratingRepo = new RatingRepository(perCtx);
//         memoryRepo = new MemoryRepository(perCtx);
        statRepo = new StatRepository(perCtx);
        gameCookieRepo = new GameCookieRepository(perCtx);

        // intialize various services
        parlorMan.init(invmgr, plreg);
        lobbyReg.init(omgr, invmgr, gameRepo);

        GameManager.setUserIdentifier(new GameManager.UserIdentifier() {
            public int getUserId (BodyObject bodyObj) {
                return ((MemberObject) bodyObj).getMemberId(); // will return 0 for guests
            }
        });
        DictionaryManager.init("data/dictionary");

        log.info("Msoy server initialized.");
    }

    @Override
    public void shutdown ()
    {
        super.shutdown();

        // shut down all active games
        for (Iterator<PlaceManager> iter = plreg.enumeratePlaceManagers(); iter.hasNext(); ) {
            PlaceManager pmgr = iter.next();
            try {
                pmgr.shutdown();
            } catch (Exception e) {
                log.log(Level.WARNING, "Place manager failed shutting down [where=" +
                        pmgr.where() + "].", e);
            }
        }
    }

    @Override // from CrowdServer
    protected PlaceRegistry createPlaceRegistry (InvocationManager invmgr, RootDObjectManager omgr)
    {
        return new PlaceRegistry(invmgr, omgr) {
            public ClassLoader getClassLoader (PlaceConfig config) {
                ClassLoader loader = hostedMan.getClassLoader(config);
                return (loader == null) ? super.getClassLoader(config) : loader;
            }
        };
    }

    @Override // from CrowdServer
    protected BodyLocator createBodyLocator ()
    {
        return new BodyLocator() {
            public BodyObject get (Name visibleName) {
                return _online.get(visibleName);
            }
        };
    }

    @Override // from PresentsServer
    protected Authenticator createAuthenticator ()
    {
        return new MsoyGameAuthenticator(memberRepo);
    }

    @Override // from PresentsServer
    protected int[] getListenPorts ()
    {
        return ServerConfig.serverPorts;
    }

    @Override // from PresentsServer
    protected void logReport (String report)
    {
        // no reports for game servers
    }

    @Override // from PresentsServer
    protected void invokerDidShutdown ()
    {
        super.invokerDidShutdown();

        // shutdown our persistence context (cache, JDBC connections)
        perCtx.shutdown();

        // close our audit logs
//         _glog.close();
    }

    /** A mapping from member name to member object for all online members. */
    protected static HashMap<MemberName,MemberObject> _online =
        new HashMap<MemberName,MemberObject>();

    protected static File _logdir = new File(ServerConfig.serverRoot, "log");
}
