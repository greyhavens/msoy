//
// $Id$

package com.threerings.msoy.server;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;

import org.apache.mina.common.IoAcceptor;

import com.google.common.collect.Maps;
import com.samskivert.jdbc.TransitionRepository;
import com.samskivert.jdbc.depot.PersistenceContext;

import com.samskivert.servlet.user.UserRepository;
import com.samskivert.util.Interval;
import com.samskivert.util.Invoker;
import com.samskivert.util.LoggingLogProvider;

import com.threerings.util.Name;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.net.AuthRequest;
import com.threerings.presents.server.Authenticator;
import com.threerings.presents.server.ClientFactory;
import com.threerings.presents.server.ClientResolver;
import com.threerings.presents.server.PresentsClient;
import com.threerings.presents.server.PresentsDObjectMgr;

import com.threerings.admin.server.ConfigRegistry;
import com.threerings.admin.server.PeeredDatabaseConfigRegistry;

import com.threerings.crowd.data.BodyObject;
import com.threerings.parlor.game.server.GameManager;

import com.threerings.whirled.server.SceneRegistry;
import com.threerings.whirled.spot.data.SpotCodes;
import com.threerings.whirled.spot.server.SpotDispatcher;
import com.threerings.whirled.spot.server.SpotProvider;

import com.whirled.game.server.DictionaryManager;

import com.threerings.msoy.admin.server.MsoyAdminManager;
import com.threerings.msoy.chat.server.ChatChannelManager;
import com.threerings.msoy.chat.server.JabberManager;
import com.threerings.msoy.game.server.MsoyGameRegistry;
import com.threerings.msoy.game.server.persist.TrophyRepository;
import com.threerings.msoy.item.server.ItemManager;
import com.threerings.msoy.notify.server.NotificationManager;
import com.threerings.msoy.peer.server.MsoyPeerManager;
import com.threerings.msoy.swiftly.server.SwiftlyManager;
import com.threerings.msoy.swiftly.server.persist.SwiftlyRepository;
import com.threerings.msoy.web.client.DeploymentConfig;
import com.threerings.msoy.web.server.MsoyHttpServer;

import com.threerings.msoy.fora.server.persist.CommentRepository;
import com.threerings.msoy.fora.server.persist.ForumRepository;
import com.threerings.msoy.fora.server.persist.IssueRepository;
import com.threerings.msoy.group.server.GroupManager;
import com.threerings.msoy.group.server.persist.GroupRepository;
import com.threerings.msoy.person.server.MailManager;

import com.threerings.msoy.world.server.MsoySceneRegistry;
import com.threerings.msoy.world.server.PetManager;
import com.threerings.msoy.world.server.persist.MsoySceneRepository;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.all.MemberName;

import static com.threerings.msoy.Log.log;

/**
 * Brings together all of the services needed by the World server.
 */
public class MsoyServer extends MsoyBaseServer
{
    /** TODO: Provides database access to the user databases. This should probably be removed. */
    public static PersistenceContext userCtx;

    /** All blocking Swiftly subversion actions must occur on this thread. */
    public static Invoker swiftlyInvoker;

    /** An invoker for sending email. */
    public static Invoker mailInvoker;

    /** Handles authentication of sessions. */
    public static MsoyAuthenticator author;

    /** Our runtime admin manager. */
    public static MsoyAdminManager adminMan = new MsoyAdminManager();

    /** Manages interactions with our peer servers. */
    public static MsoyPeerManager peerMan = new MsoyPeerManager();

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

    /** Our runtime jabber manager. */
    public static JabberManager jabberMan = new JabberManager();

    /** Contains information on our groups. */
    public static GroupRepository groupRepo;

    /** Contains information on our forums. */
    public static ForumRepository forumRepo;

    /** Contains information on our issues. */
    public static IssueRepository issueRepo;

    /** Contains member comments on various things. */
    public static CommentRepository commentRepo;

    /** Provides access to our trophy metadata. */
    public static TrophyRepository trophyRepo;

    /** Contains information on our swiftly projects. */
    public static SwiftlyRepository swiftlyRepo;

    /** The Msoy scene repository. */
    public static MsoySceneRepository sceneRepo;

    /** The Msoy item manager. */
    public static ItemManager itemMan = new ItemManager();

    /** Provides spot-related services. */
    public static SpotProvider spotProv;

    /** Our runtime swiftly editor manager. */
    public static SwiftlyManager swiftlyMan = new SwiftlyManager();

    /** Manages our external game servers. */
    public static MsoyGameRegistry gameReg = new MsoyGameRegistry();

    /** Handles HTTP servlet requests. */
    public static MsoyHttpServer httpServer;

    /** Handles our cuddly little pets. */
    public static PetManager petMan = new PetManager();

    /** Handles notifications to clients. */
    public static NotificationManager notifyMan = new NotificationManager();

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
     * Returns an <i>unmodifiable</i> collection of members currently online.
     * This should only be called from the dobjmgr thread.
     */
    public static Collection<MemberObject> getMembersOnline ()
    {
        requireDObjThread();
        return Collections.unmodifiableCollection(_online.values());
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
     * Returns true if we are running in a World server.
     */
    public static boolean isActive ()
    {
        return (mailInvoker != null);
    }

    /**
     * Starts everything a runnin'.
     */
    public static void main (String[] args)
    {
        // route legacy logs through the Java log system
        com.samskivert.util.Log.setLogProvider(new LoggingLogProvider());

        // if we're on the dev server, up our long invoker warning to 3 seconds
        if (ServerConfig.config.getValue("auto_restart", false)) {
            Invoker.setDefaultLongThreshold(3000L);
        }

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
        super.init();

        // we use this on dev to work with the dev ooouser database; TODO: nix
        userCtx = new PersistenceContext(
            UserRepository.USER_REPOSITORY_IDENT, _conProv, perCtx.getCacheAdapter());

        // set up the right client factory
        clmgr.setClientFactory(new ClientFactory() {
            public PresentsClient createClient (AuthRequest areq) {
                return new MsoyClient(_eventLog);
            }
            public ClientResolver createClientResolver (Name username) {
                return new MsoyClientResolver();
            }
        });

        // this is not public because it should not be referenced statically, it should always be
        // passed in to whatever manager needs to handle transitions
        _transitRepo = new TransitionRepository(_conProv);

        // create our various repositories
        groupRepo = new GroupRepository(perCtx, _eventLog);
        forumRepo = new ForumRepository(perCtx);
        issueRepo = new IssueRepository(perCtx);
        commentRepo = new CommentRepository(perCtx);
        trophyRepo = new TrophyRepository(perCtx);
        swiftlyRepo = new SwiftlyRepository(perCtx);

        // initialize the swiftly invoker
        swiftlyInvoker = new Invoker("swiftly_invoker", omgr);
        swiftlyInvoker.setDaemon(true);
        swiftlyInvoker.start();

        // initialize the mail invoker
        mailInvoker = new Invoker("mail_invoker", omgr);
        mailInvoker.setDaemon(true);
        mailInvoker.start();
    }

    @Override
    public void shutdown ()
    {
        super.shutdown();

        // shut down our http server
        try {
            httpServer.stop();
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to stop http server.", e);
        }

        // and our policy server if one is running
        if (_policyServer != null) {
            _policyServer.unbindAll();
        }
    }

    @Override // from MsoyBaseServer
    protected String getIdent ()
    {
        return ServerConfig.nodeName;
    }

    @Override // from MsoyBaseServer
    protected ConfigRegistry createConfigRegistry ()
        throws Exception
    {
        return new PeeredDatabaseConfigRegistry(perCtx, invoker, peerMan);
    }

    @Override // from WhirledServer
    protected SceneRegistry createSceneRegistry ()
        throws Exception
    {
        return new MsoySceneRegistry(invmgr, sceneRepo = new MsoySceneRepository(perCtx), _eventLog);
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
        // TODO: export this information via JMX
    }

    /**
     * Called once our runtime configuration information is loaded and ready.
     */
    protected void finishInit ()
        throws Exception
    {
        // initialize our authenticator
        author.init(_eventLog);

        // start up our peer manager
        log.info("Running in cluster mode as node '" + ServerConfig.nodeName + "'.");
        peerMan.init(perCtx, invoker, ServerConfig.nodeName, ServerConfig.sharedSecret,
                     ServerConfig.backChannelHost, ServerConfig.serverHost, getListenPorts()[0]);

        // intialize various services
        spotProv = new SpotProvider(omgr, plreg, screg);
        invmgr.registerDispatcher(new SpotDispatcher(spotProv), SpotCodes.WHIRLED_GROUP);
        adminMan.init(this, _eventLog);
        memberMan.init(memberRepo, groupRepo);
        friendMan.init();
        groupMan.init(groupRepo, memberRepo);
        mailMan.init(perCtx, memberRepo, _eventLog);
        channelMan.init(invmgr);
        jabberMan.init(invmgr);
        itemMan.init(perCtx, _eventLog);
        swiftlyMan.init(invmgr);
        petMan.init(invmgr);
        gameReg.init(invmgr, itemMan.getGameRepository());

        GameManager.setUserIdentifier(new GameManager.UserIdentifier() {
            public int getUserId (BodyObject bodyObj) {
                return ((MemberObject) bodyObj).getMemberId(); // will return 0 for guests
            }
        });
        DictionaryManager.init("data/dictionary");

        sceneRepo.finishInit(itemMan.getDecorRepository());

        // create and start up our HTTP server
        httpServer = new MsoyHttpServer(_logdir, _eventLog);
        httpServer.start();

        // if we're a dev deployment and our policy port is not privileged, run the policy server
        // right in the msoy server to simplify life for developers
        if (DeploymentConfig.devDeployment && ServerConfig.socketPolicyPort > 1024) {
            _policyServer = PolicyServer.init();
        }

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

        // resolve any remaining database schemas that have not yet been loaded
        if (!ServerConfig.config.getValue("depot.lazy_init", true)) {
            perCtx.initializeManagedRecords(true);
            userCtx.initializeManagedRecords(true);
        }

        log.info("Msoy server initialized.");
    }

    @Override // from PresentsServer
    protected void invokerDidShutdown ()
    {
        super.invokerDidShutdown();

        // shutdown our persistence context (cache, JDBC connections)
        userCtx.shutdown();
    }

    /**
     * Check the filesystem and return the newest timestamp for any of our code jars. This method
     * should remain safe to run on any thread.
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

    /** Used to auto-restart the development server when its code is updated. */
    protected long _codeModified;

    /** A policy server used on dev deployments. */
    protected IoAcceptor _policyServer;

    /** Our transition repository. */
    protected static TransitionRepository _transitRepo;

    /** A mapping from member name to member object for all online members. */
    protected static HashMap<MemberName,MemberObject> _online = Maps.newHashMap();

    /** Check for modified code every 30 seconds. */
    protected static final long AUTO_RESTART_CHECK_INTERVAL = 30 * 1000L;
}
