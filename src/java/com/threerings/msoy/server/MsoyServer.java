//
// $Id$

package com.threerings.msoy.server;

import java.io.File;
import java.security.Security;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;

import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.StaticConnectionProvider;
import com.samskivert.jdbc.TransitionRepository;
import com.samskivert.jdbc.depot.PersistenceContext;

import com.samskivert.servlet.user.UserRepository;
import com.samskivert.util.AuditLogger;
import com.samskivert.util.Interval;
import com.samskivert.util.Invoker;
import com.samskivert.util.LoggingLogProvider;
import com.samskivert.util.OneLineLogFormatter;

import com.threerings.util.MessageManager;
import com.threerings.util.Name;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.RootDObjectManager;
import com.threerings.presents.net.AuthRequest;
import com.threerings.presents.server.Authenticator;
import com.threerings.presents.server.ClientFactory;
import com.threerings.presents.server.ClientResolver;
import com.threerings.presents.server.InvocationManager;
import com.threerings.presents.server.PresentsClient;
import com.threerings.presents.server.PresentsDObjectMgr;

import com.threerings.admin.server.AdminProvider;
import com.threerings.admin.server.ConfigRegistry;
import com.threerings.admin.server.DatabaseConfigRegistry;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.server.PlaceManager;
import com.threerings.crowd.server.PlaceRegistry;

import com.threerings.ezgame.server.DictionaryManager;
import com.threerings.ezgame.server.persist.GameCookieRepository;

import com.threerings.parlor.game.server.GameManager;
import com.threerings.parlor.rating.server.persist.RatingRepository;
import com.threerings.parlor.server.ParlorManager;

import com.threerings.whirled.server.SceneRegistry;
import com.threerings.whirled.server.WhirledServer;
import com.threerings.whirled.spot.data.SpotCodes;
import com.threerings.whirled.spot.server.SpotDispatcher;
import com.threerings.whirled.spot.server.SpotProvider;

import com.threerings.stats.server.persist.StatRepository;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.admin.server.MsoyAdminManager;
import com.threerings.msoy.admin.server.RuntimeConfig;
import com.threerings.msoy.chat.server.ChatChannelManager;
import com.threerings.msoy.game.server.HostedGameManager;
import com.threerings.msoy.game.server.LobbyRegistry;
import com.threerings.msoy.game.server.WorldGameRegistry;
import com.threerings.msoy.item.server.ItemManager;
import com.threerings.msoy.notify.server.NotificationManager;
import com.threerings.msoy.peer.server.MsoyPeerManager;
import com.threerings.msoy.person.server.MailManager;
import com.threerings.msoy.person.server.persist.ProfileRepository;
import com.threerings.msoy.swiftly.server.SwiftlyManager;
import com.threerings.msoy.web.server.MsoyHttpServer;
import com.threerings.msoy.world.server.MsoySceneRegistry;
import com.threerings.msoy.world.server.PetManager;

import com.threerings.msoy.server.persist.GroupRepository;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.swiftly.server.persist.SwiftlyRepository;
import com.threerings.msoy.world.server.persist.MemoryRepository;
import com.threerings.msoy.world.server.persist.MsoySceneRepository;

import static com.threerings.msoy.Log.log;

/**
 * Msoy server class.
 */
public class MsoyServer extends WhirledServer
{
    /** Provides database access to all of our repositories. */
    public static PersistenceContext perCtx;
    
    /** TODO: Provides database access to the user databases. This should probably be removed. */
    public static PersistenceContext userCtx;

    /** Maintains a registry of runtime configuration information. */
    public static ConfigRegistry confReg;

    /** Provides a mechanism for translating strings on the server. <em>Note:</em> avoid using this
     * if at all possible. Delay translation to the client so that we can properly react to the
     * client's locale. Translating on the server means that we treat all clients as if they are
     * using the default locale of the server. */
    public static MessageManager msgMan = new MessageManager("rsrc.i18n");

    /** All blocking Swiftly subversion actions must occur on this thread. */
    public static Invoker swiftlyInvoker;

    /** An invoker for sending email. */
    public static Invoker mailInvoker;

    /** Handles authentication of sessions. */
    public static MsoyAuthenticator author;

    /** Our runtime admin manager. */
    public static MsoyAdminManager adminMan = new MsoyAdminManager();

    /** Manages interactions with our peer servers. */
    public static MsoyPeerManager peerMan;

    /** Our runtime member manager. */
    public static MemberManager memberMan = new MemberManager();

    /** Handles management of member's friends lists. */
    public static FriendManager friendMan = new FriendManager();

    /** Our runtime group manager. */
    public static GroupManager groupMan = new GroupManager();

    /** Our runtime mail manager. */
    public static MailManager mailMan = new MailManager();

    /** Our runtime chat channel manager. */
    public static ChatChannelManager channelMan = new ChatChannelManager();

    /** Contains information on our members. */
    public static MemberRepository memberRepo;

    /** Contains information on our member profiles. */
    public static ProfileRepository profileRepo;

    /** Contains information on our groups. */
    public static GroupRepository groupRepo;

    /** Contains information on our swiftly projects. */
    public static SwiftlyRepository swiftlyRepo;

    /** Contains the rating data for each player and game. */
    public static RatingRepository ratingRepo;

    /** The Msoy scene repository. */
    public static MsoySceneRepository sceneRepo;

    /** Maintains "smart" digital item memories. */
    public static MemoryRepository memoryRepo;

    /** Manages the persistent repository of stats. */
    public static StatRepository statRepo;

    /** Stores user's game cookies. */
    public static GameCookieRepository gameCookieRepo;

    /** The Msoy item manager. */
    public static ItemManager itemMan = new ItemManager();

    /** Provides spot-related services. */
    public static SpotProvider spotProv;

    /** The parlor manager in operation on this server. */
    public static ParlorManager parlorMan = new ParlorManager();

    /** Our runtime swiftly editor manager. */
    public static SwiftlyManager swiftlyMan = new SwiftlyManager();

    /** The lobby registry for this server. */
    public static LobbyRegistry lobbyReg = new LobbyRegistry();

    /** The in-world game registry for this server. */
    public static WorldGameRegistry worldGameReg = new WorldGameRegistry();

    /** Our transition repository. */
    public static TransitionRepository transitRepo;

    /** Handles HTTP servlet requests. */
    public static MsoyHttpServer httpServer;

    /** Handles sandboxed game server code. */
    public static HostedGameManager hostedMan = new HostedGameManager();

    /** Handles our cuddly little pets. */
    public static PetManager petMan = new PetManager();

    /** Handles notifications to clients. */
    public static NotificationManager notifyMan = new NotificationManager();

    /**
     * Creates an audit log with the specified name (which should not include
     * the <code>.log</code> suffix) in our server log directory.
     */
    public static AuditLogger createAuditLog (String logname)
    {
        // qualify our log file with the nodename to avoid collisions
        if (ServerConfig.nodeName != null) {
            logname = logname + "_" + ServerConfig.nodeName;
        }
        return new AuditLogger(_logdir, logname + ".log");
    }

    /**
     * Logs a message to the general audit log.
     */
    public static void generalLog (String message)
    {
        _glog.log(message);
    }

    /**
     * Logs a message to the item audit log.
     */
    public static void itemLog (String message)
    {
        _ilog.log(message);
    }

    /**
     * Logs a message to the flow audit log.
     */
    public static void flowLog (String message)
    {
        _flog.log(message);
    }

    /**
     * Returns the member object for the user identified by the given ID if they are online
     * currently, null otherwise. This should only be called from the dobjmgr thread.
     */
    public static MemberObject lookupMember (int memberId)
    {
        // We can't look up guests this way, as they all have the same memberId
        if (memberId == MemberName.GUEST_ID) {
            return null;
        }
        // MemberName.equals and hashCode only depend on the id
        return lookupMember(new MemberName(null, memberId));
    }

    /**
     * Returns the member object for the user identified by the given name if they are online
     * currently, null otherwise. This should only be called from the dobjmgr thread.
     */
    public static MemberObject lookupMember (MemberName name)
    {
        requireDObjThread();
        return _online.get(name);
    }

    /**
     * Called when a member starts their session to associate the name with the member's
     * distributed object.
     */
    public static void memberLoggedOn (MemberObject memobj)
    {
        _online.put(memobj.memberName, memobj);
        memberMan.memberLoggedOn(memobj);
        friendMan.memberLoggedOn(memobj);

        // update our members online count in the status object
        adminMan.statObj.setMembersOnline(clmgr.getClientCount());
    }

    /**
     * Called when a member ends their session to clear their name to member object mapping.
     */
    public static void memberLoggedOff (MemberObject memobj)
    {
        _online.remove(memobj.memberName);
        friendMan.memberLoggedOff(memobj);

        // update our members online count in the status object
        adminMan.statObj.setMembersOnline(clmgr.getClientCount());
    }

    /**
     * Ensures that the calling thread is the distributed object event dispatch thread, throwing an
     * {@link IllegalStateException} if it is not.
     */
    public static void requireDObjThread ()
    {
        if (!omgr.isDispatchThread()) {
            String errmsg = "This method must be called on the distributed object thread.";
            throw new IllegalStateException(errmsg);
        }
    }

    /**
     * Ensures that the calling thread <em>is not</em> the distributed object event dispatch
     * thread, throwing an {@link IllegalStateException} if it is.
     */
    public static void refuseDObjThread ()
    {
        if (omgr.isDispatchThread()) {
            String errmsg = "This method must not be called on the distributed object thread.";
            throw new IllegalStateException(errmsg);
        }
    }

    /**
     * Starts everything a runnin'.
     */
    public static void main (String[] args)
    {
        // set up the proper logging services
        com.samskivert.util.Log.setLogProvider(new LoggingLogProvider());
        OneLineLogFormatter.configureDefaultHandler();

        MsoyServer server = new MsoyServer();
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
        // before doing anything else, let's ensure that we don't cache DNS queries forever -- this
        // breaks Amazon S3, specifically.
        Security.setProperty("networkaddress.cache.ttl" , "30");

        // create our JDBC bits before calling super.init() because our superclass will attempt to
        // create our authenticator and we need that ready by then
        _conProv = new StaticConnectionProvider(ServerConfig.getJDBCConfig());
        perCtx = new PersistenceContext("msoy", _conProv, ServerConfig.createCacheAdapter());

        userCtx = new PersistenceContext(
            UserRepository.USER_REPOSITORY_IDENT, _conProv, ServerConfig.createCacheAdapter());

        // create our transition manager prior to doing anything else
        transitRepo = new TransitionRepository(_conProv);

        super.init();

        // set up the right client factory
        clmgr.setClientFactory(new ClientFactory() {
            public PresentsClient createClient (AuthRequest areq) {
                return new MsoyClient();
            }
            public ClientResolver createClientResolver (Name username) {
                return new MsoyClientResolver();
            }
        });

        // set up our default object access controller
        omgr.setDefaultAccessController(MsoyObjectAccess.DEFAULT);

        // create our various repositories
        memberRepo = new MemberRepository(perCtx);
        profileRepo = new ProfileRepository(perCtx);
        groupRepo = new GroupRepository(perCtx);
        swiftlyRepo = new SwiftlyRepository(perCtx);
        ratingRepo = new RatingRepository(perCtx);
        memoryRepo = new MemoryRepository(perCtx);
        statRepo = new StatRepository(perCtx);
        gameCookieRepo = new GameCookieRepository(perCtx);

        // create and set up our configuration registry and admin service
        confReg = new DatabaseConfigRegistry(perCtx, invoker);
        AdminProvider.init(invmgr, confReg);

        // start up our peer manager
        log.info("Running in cluster mode as node '" + ServerConfig.nodeName + "'.");
        peerMan = new MsoyPeerManager(perCtx, invoker);

        // initialize the swiftly invoker
        swiftlyInvoker = new Invoker("swiftly_invoker", omgr);
        swiftlyInvoker.setDaemon(true);
        swiftlyInvoker.start();

        // initialize the mail invoker
        mailInvoker = new Invoker("mail_invoker", omgr);
        mailInvoker.setDaemon(true);
        mailInvoker.start();

        // now initialize our runtime configuration, postponing the remaining server initialization
        // until our configuration objects are available
        RuntimeConfig.init(omgr);
        omgr.postRunnable(new PresentsDObjectMgr.LongRunnable () {
            public void run () {
                try {
                    finishInit();
                } catch (Exception e) {
                    log.log(Level.WARNING, "Server initialization failed.", e);
                    System.exit(-1);
                }
            }
        });
    }

    @Override
    public void shutdown ()
    {
        super.shutdown();

        // shut down all active games and rooms
        for (Iterator<PlaceManager> iter = plreg.enumeratePlaceManagers(); iter.hasNext(); ) {
            PlaceManager pmgr = iter.next();
            try {
                pmgr.shutdown();
            } catch (Exception e) {
                log.log(Level.WARNING, "Place manager failed shutting down [where=" +
                        pmgr.where() + "].", e);
            }
        }

        // shut down our http server
        try {
            httpServer.stop();
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to stop http server.", e);
        }
    }

    @Override // from WhirledServer
    protected SceneRegistry createSceneRegistry ()
        throws Exception
    {
        return new MsoySceneRegistry(invmgr, sceneRepo = new MsoySceneRepository(perCtx));
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
        return (author = new MsoyAuthenticator());
    }

    @Override // from PresentsServer
    protected int[] getListenPorts ()
    {
        return ServerConfig.serverPorts;
    }

    @Override // from PresentsServer
    protected void logReport (String report)
    {
        _stlog.log(report);
    }

    /**
     * Called once our runtime configuration information is loaded and ready.
     */
    protected void finishInit ()
        throws Exception
    {
        // intialize various services
        spotProv = new SpotProvider(omgr, plreg, screg);
        invmgr.registerDispatcher(new SpotDispatcher(spotProv), SpotCodes.WHIRLED_GROUP);
        parlorMan.init(invmgr, plreg);
        adminMan.init(this);
        if (peerMan != null) {
            peerMan.init(ServerConfig.nodeName, ServerConfig.sharedSecret,
                         ServerConfig.backChannelHost, ServerConfig.serverHost,
                         getListenPorts()[0]);
        }
        memberMan.init(memberRepo, groupRepo);
        friendMan.init();
        groupMan.init(groupRepo, memberRepo);
        mailMan.init(perCtx, memberRepo);
        channelMan.init(invmgr);
        itemMan.init(perCtx);
        swiftlyMan.init(invmgr);
        petMan.init(invmgr);
        lobbyReg.init(omgr, invmgr, itemMan.getGameRepository());
        worldGameReg.init(invmgr);

        GameManager.setUserIdentifier(new GameManager.UserIdentifier() {
            public int getUserId (BodyObject bodyObj) {
                return ((MemberObject) bodyObj).getMemberId(); // will return 0 for guests
            }
        });
        DictionaryManager.init("data/dictionary");

        sceneRepo.finishInit(itemMan.getDecorRepository());

        // create and start up our HTTP server
        httpServer = new MsoyHttpServer(_logdir);
        httpServer.start();

        // start up an interval that checks to see if our code has changed and auto-restarts the
        // server as soon as possible when it has
        if (ServerConfig.config.getValue("auto_restart", false)) {
            _codeModified = codeModifiedTime();
            new Interval() { // Note well: this interval does not run on the dobj thread
                public void expired () {
                    // ...we simply post a LongRunnable to do the job
                    omgr.postRunnable(new PresentsDObjectMgr.LongRunnable() {
                        public void run () {
                            checkAutoRestart();
                        }
                    });
                }
                public String toString () {
                    return "checkAutoRestart interval";
                }
            }.schedule(AUTO_RESTART_CHECK_INTERVAL, true);
        }

        log.info("Msoy server initialized.");
    }

    @Override // from PresentsServer
    protected void invokerDidShutdown ()
    {
        super.invokerDidShutdown();

        // shutdown our persistence context (cache, JDBC connections)
        perCtx.shutdown();
        userCtx.shutdown();

        // close our audit logs
        _glog.close();
        _ilog.close();
        _stlog.close();
    }

    /**
     * Check the filesystem and return the newest timestamp for any of
     * our code jars. This method should remain safe to run on any thread.
     */
    protected long codeModifiedTime ()
    {
        // just the one...
        return new File(ServerConfig.serverRoot, "dist/msoy-code.jar").lastModified();
    }

    /**
     * Check to see if the server should be restarted.
     */
    protected void checkAutoRestart ()
    {
        // look up the last-modified time
        long lastModified = codeModifiedTime();
        if (lastModified <= _codeModified || adminMan.statObj.serverRebootTime != 0L) {
            return;
        }

        // if someone is online, give 'em two minutes, otherwise reboot immediately
        boolean playersOnline = false;
        for (Iterator<ClientObject> iter = clmgr.enumerateClientObjects(); iter.hasNext(); ) {
            if (iter.next() instanceof MemberObject) {
                playersOnline = true;
                break;
            }
        }
        adminMan.scheduleReboot(playersOnline ? 2 : 0, "codeUpdateAutoRestart");
    }

    /** A mapping from member name to member object for all online members. */
    protected static HashMap<MemberName,MemberObject> _online =
        new HashMap<MemberName,MemberObject>();

    /** Used to auto-restart the development server when its code is updated. */
    protected long _codeModified;

    /** The connection provider used to access our JDBC databases. Don't use this; rather use
     * {@link #perCtx}. */
    protected static ConnectionProvider _conProv;

    protected static File _logdir = new File(ServerConfig.serverRoot, "log");
    protected static AuditLogger _glog = createAuditLog("server");
    protected static AuditLogger _ilog = createAuditLog("item");
    protected static AuditLogger _flog = createAuditLog("flow");
    protected static AuditLogger _stlog = createAuditLog("state");

    /** Check for modified code every 30 seconds. */
    protected static final long AUTO_RESTART_CHECK_INTERVAL = 30 * 1000L;
}
