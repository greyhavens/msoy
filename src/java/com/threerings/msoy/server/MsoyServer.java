//
// $Id$

package com.threerings.msoy.server;

import java.io.File;
import java.security.Security;
import java.util.HashMap;
import java.util.logging.Level;

import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.StaticConnectionProvider;
import com.samskivert.jdbc.TransitionRepository;
import com.samskivert.util.AuditLogger;
import com.samskivert.util.LoggingLogProvider;
import com.samskivert.util.OneLineLogFormatter;

import com.threerings.util.Name;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.RootDObjectManager;
import com.threerings.presents.net.AuthRequest;
import com.threerings.presents.server.Authenticator;
import com.threerings.presents.server.ClientFactory;
import com.threerings.presents.server.ClientResolver;
import com.threerings.presents.server.InvocationManager;
import com.threerings.presents.server.PresentsClient;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.server.PlaceManager;
import com.threerings.crowd.server.PlaceRegistry;

import com.threerings.parlor.server.ParlorManager;

import com.threerings.whirled.server.SceneRegistry;
import com.threerings.whirled.server.WhirledServer;
import com.threerings.whirled.server.persist.SceneRepository;
import com.threerings.whirled.spot.server.SpotDispatcher;
import com.threerings.whirled.spot.server.SpotProvider;
import com.threerings.whirled.util.SceneFactory;

import com.threerings.toybox.server.ToyBoxManager;

import com.threerings.msoy.data.MemberName;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.item.server.ItemManager;
import com.threerings.msoy.game.server.LobbyRegistry;
import com.threerings.msoy.person.server.PersonPageManager;
import com.threerings.msoy.web.server.MsoyHttpServer;
import com.threerings.msoy.world.data.RoomConfig;

import com.threerings.msoy.person.server.persist.PersonPageRepository;
import com.threerings.msoy.server.persist.GroupRepository;
import com.threerings.msoy.server.persist.MailRepository;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.server.persist.MsoySceneRepository;
import com.threerings.msoy.server.persist.ProfileRepository;

import static com.threerings.msoy.Log.log;

/**
 * Msoy server class.
 */
public class MsoyServer extends WhirledServer
{
    /** The connection provider used to access our JDBC databases. */
    public static ConnectionProvider conProv;

    /** Our runtime member manager. */
    public static MemberManager memberMan = new MemberManager();

    /** Our runtime mail manager. */
    public static MailManager mailMan = new MailManager();

    /** Contains information on our members. */
    public static MemberRepository memberRepo;

    /** The Msoy scene repository. */
    public static MsoySceneRepository sceneRepo;

    /** The Msoy item manager. */
    public static ItemManager itemMan = new ItemManager();

    /** The Msoy person page manager. */
    public static PersonPageManager ppageMan = new PersonPageManager();

    /** Provides spot-related services. */
    public static SpotProvider spotProv;

    /** The parlor manager in operation on this server. */
    public static ParlorManager parlorMan = new ParlorManager();

    /** The lobby registry for this server. */
    public static LobbyRegistry lobbyReg = new LobbyRegistry();

    /** Our transition repository. */
    public static TransitionRepository transitRepo;

    /** Handles HTTP servlet requests. */
    public static MsoyHttpServer httpServer;

    /** Handles sandboxed game server code. */
    public static ToyBoxManager toyMan = new ToyBoxManager();

    /**
     * Creates an audit log with the specified name (which should not include
     * the <code>.log</code> suffix) in our server log directory.
     */
    public static AuditLogger createAuditLog (String logname)
    {
        return new AuditLogger(_logdir, logname + ".log");
    }

    /**
     * Loads a message to the general audit log.
     */
    public static void generalLog (String message)
    {
        _glog.log(message);
    }

    /**
     * Loads a message to the item audit log.
     */
    public static void itemLog (String message)
    {
        _ilog.log(message);
    }

    /**
     * Returns the member object for the user identified by the given ID if they are online
     * currently, null otherwise. This should only be called from the dobjmgr thread.
     */
    public static MemberObject lookupMember (int memberId)
    {
        // MemberName.equals and hashCode only depend on the id
        return lookupMember(new MemberName(null, memberId));
    }

    /**
     * Returns the member object for the user identified by the given name if they are online
     * currently, null otherwise. This should only be called from the dobjmgr thread.
     */
    public static MemberObject lookupMember (MemberName name)
    {
        return _online.get(name);
    }

    /**
     * Called when a member starts their session to associate the name with the
     * member's distributed object.
     */
    public static void registerMember (MemberObject member)
    {
        _online.put(member.memberName, member);
    }

    /**
     * Called when a member ends their session to clear their name to member
     * object mapping.
     */
    public static void clearMember (MemberObject member)
    {
        _online.remove(member.memberName);
    }

    @Override
    public void init ()
        throws Exception
    {
        // before doing anything else, let's ensure that we don't cache DNS
        // queries forever -- this breaks Amazon S3, specifically.
        Security.setProperty("networkaddress.cache.ttl" , "30");

        // create our connection provider before calling super.init() because
        // our superclass will attempt to create our authenticator and we'll
        // need the connection provider ready at that time
        conProv = new StaticConnectionProvider(ServerConfig.getJDBCConfig());

        // create our transition manager prior to doing anything else
        transitRepo = new TransitionRepository(conProv);

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
        PersonPageRepository ppageRepo = new PersonPageRepository();
        memberRepo = new MemberRepository(conProv);
        ProfileRepository profileRepo = new ProfileRepository(conProv);
        GroupRepository groupRepo = new GroupRepository(conProv);
        MailRepository mailRepo = new MailRepository(conProv);

        // intialize various services
        spotProv = new SpotProvider(omgr, plreg, screg);
        invmgr.registerDispatcher(new SpotDispatcher(spotProv), true);
        parlorMan.init(invmgr, plreg);
        sceneRepo = (MsoySceneRepository) _screp;
        memberMan.init(memberRepo, profileRepo, groupRepo);
        mailMan.init(mailRepo, memberRepo);
        itemMan.init(conProv);
        ppageMan.init(ppageRepo);
        lobbyReg.init(invmgr);
        toyMan.init(omgr, invoker, invmgr, plreg, itemMan.getGameRepository());

        // create and start up our HTTP server
        httpServer = new MsoyHttpServer();
        httpServer.init();

        log.info("Msoy server initialized.");
    }

    @Override
    public void shutdown ()
    {
        super.shutdown();

        // shut down our http server
        try {
            httpServer.stop(true);
        } catch (InterruptedException ie) {
            log.log(Level.WARNING, "Failed to stop http server.", ie);
        }

        // close our audit logs
        _glog.close();
        _ilog.close();
        _stlog.close();
    }

    @Override
    protected SceneRepository createSceneRepository ()
        throws Exception
    {
        return new MsoySceneRepository(conProv);
    }

    @Override
    protected SceneFactory createSceneFactory ()
        throws Exception
    {
        return _sceneFactory;
    }

    @Override
    protected SceneRegistry.ConfigFactory createConfigFactory ()
        throws Exception
    {
        return _sceneFactory;
    }

    @Override
    protected Authenticator createAuthenticator ()
    {
        return new MsoyAuthenticator();
    }

    @Override
    protected PlaceRegistry createPlaceRegistry (
        InvocationManager invmgr, RootDObjectManager omgr)
    {
        return new PlaceRegistry(invmgr, omgr) {
            public ClassLoader getClassLoader (PlaceConfig config) {
                ClassLoader loader = toyMan.getClassLoader(config);
                return (loader == null) ? super.getClassLoader(config) : loader;
            }
        };
    }

    @Override
    protected int[] getListenPorts ()
    {
        return ServerConfig.serverPorts;
    }

    @Override
    protected void logReport (String report)
    {
        _stlog.log(report);
    }

    @Override // documentation inherited
    protected BodyLocator createBodyLocator ()
    {
        return new BodyLocator() {
            public BodyObject get (Name visibleName) {
                return _online.get(visibleName);
            }
        };
    }

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

    /** Our scene and config factory. */
    protected MsoySceneFactory _sceneFactory = new MsoySceneFactory();

    /** A mapping from member name to member object for all online members. */
    protected static HashMap<MemberName,MemberObject> _online =
        new HashMap<MemberName,MemberObject>();

    protected static File _logdir = new File(ServerConfig.serverRoot, "log");
    protected static AuditLogger _glog = createAuditLog("server");
    protected static AuditLogger _ilog = createAuditLog("item");
    protected static AuditLogger _stlog = createAuditLog("state");
}
