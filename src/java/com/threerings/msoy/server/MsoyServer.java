//
// $Id$

package com.threerings.msoy.server;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.mina.common.IoAcceptor;

import com.google.common.collect.Maps;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import com.samskivert.jdbc.depot.PersistenceContext;

import com.samskivert.servlet.user.UserRepository;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.Interval;
import com.samskivert.util.Invoker;
import com.samskivert.util.LoggingLogProvider;

import com.threerings.util.Name;

import com.threerings.presents.annotation.EventThread;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.net.AuthRequest;
import com.threerings.presents.server.Authenticator;
import com.threerings.presents.server.ClientFactory;
import com.threerings.presents.server.ClientResolver;
import com.threerings.presents.server.PresentsClient;
import com.threerings.presents.server.PresentsDObjectMgr;
import com.threerings.presents.server.PresentsServer;
import com.threerings.presents.server.ReportManager;
import com.threerings.presents.server.ShutdownManager;

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
import com.threerings.msoy.group.server.persist.GroupRepository;
import com.threerings.msoy.person.server.MailManager;
import com.threerings.msoy.person.server.persist.MailRepository;
import com.threerings.msoy.underwire.server.MsoyUnderwireManager;

import com.threerings.msoy.world.server.MsoySceneRegistry;
import com.threerings.msoy.world.server.PetManager;
import com.threerings.msoy.world.server.persist.MsoySceneRepository;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.all.MemberName;

import static com.threerings.msoy.Log.log;

/**
 * Brings together all of the services needed by the World server.
 */
@Singleton
public class MsoyServer extends MsoyBaseServer
    implements ShutdownManager.Shutdowner
{
    /** Configures dependencies needed by the world server. */
    public static class Module extends MsoyBaseServer.Module
    {
        @Override protected void configure () {
            super.configure();
            // nada (yet)
        }
    }

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
    public static MsoyPeerManager peerMan;

    /** Our runtime member manager. */
    public static MemberManager memberMan = new MemberManager();

    /** Handles management of member's friends lists. */
    public static FriendManager friendMan = new FriendManager();

    /** Handles mail related services. */
    public static MailManager mailMan = new MailManager();

    /** Our runtime chat channel manager. */
    public static ChatChannelManager channelMan = new ChatChannelManager();

    /** Our runtime support manager. */
    public static MsoyUnderwireManager supportMan = new MsoyUnderwireManager();

    /** Contains information on our groups. */
    public static GroupRepository groupRepo;

    /** Contains information on our mail system. */
    public static MailRepository mailRepo;

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
    public static SwiftlyManager swiftlyMan;

    /** Manages our external game servers. */
    public static MsoyGameRegistry gameReg;

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
    @EventThread
    public static MemberObject lookupMember (int memberId)
    {
        requireDObjThread();
        return _online.get(memberId);
    }

    /**
     * Returns the member object for the user identified by the given name if they are online
     * currently, null otherwise. This should only be called from the dobjmgr thread.
     */
    @EventThread
    public static MemberObject lookupMember (MemberName name)
    {
        return lookupMember(name.getMemberId());
    }

    /**
     * Returns an <i>unmodifiable</i> collection of members currently online.
     * This should only be called from the dobjmgr thread.
     */
    @EventThread
    public static Collection<MemberObject> getMembersOnline ()
    {
        requireDObjThread();
        return Collections.unmodifiableCollection(_online.values());
    }

    /**
     * Called when a member starts their session to associate the name with the member's
     * distributed object.
     */
    @EventThread
    public static void memberLoggedOn (MemberObject memobj)
    {
        _online.put(memobj.memberName.getMemberId(), memobj);
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
        _online.remove(memobj.memberName.getMemberId());
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
        // if we're on the dev server, up our long invoker warning to 3 seconds
        if (ServerConfig.config.getValue("auto_restart", false)) {
            Invoker.setDefaultLongThreshold(3000L);
        }

        Injector injector = Guice.createInjector(new Module());
        MsoyServer server = injector.getInstance(MsoyServer.class);
        try {
            server.init(injector);
            server.run();
        } catch (Exception e) {
            log.warning("Unable to initialize server", e);
            System.exit(255);
        }
    }

    // from interface ShutdownManager.Shutdowner
    public void shutdown ()
    {
        // shut down our http server
        try {
            httpServer.stop();
        } catch (Exception e) {
            log.warning("Failed to stop http server.", e);
        }

        // and our policy server if one is running
        if (_policyServer != null) {
            _policyServer.unbindAll();
        }
    }

    @Override // from MsoyBaseServer
    public void init (Injector injector)
        throws Exception
    {
        super.init(injector);

        // TEMP: publish some managers via the legacy static references
        peerMan = _peerMan;
        gameReg = _gameReg;
        swiftlyMan = _swiftlyMan;
        sceneRepo = _sceneRepo;
        adminMan = _adminMan;

        // we need to know when we're shutting down
        _shutmgr.registerShutdowner(this);

        // we use this on dev to work with the dev ooouser database; TODO: nix
        userCtx = new PersistenceContext(UserRepository.USER_REPOSITORY_IDENT,
                                         ServerConfig.createConnectionProvider(),
                                         _perCtx.getCacheAdapter());

        // set up the right client factory
        clmgr.setClientFactory(new ClientFactory() {
            public PresentsClient createClient (AuthRequest areq) {
                return new MsoyClient(_eventLog);
            }
            public ClientResolver createClientResolver (Name username) {
                return new MsoyClientResolver();
            }
        });

        // create our various repositories
        groupRepo = new GroupRepository(_perCtx, _eventLog);
        mailRepo = new MailRepository(_perCtx, _eventLog);
        forumRepo = new ForumRepository(_perCtx);
        issueRepo = new IssueRepository(_perCtx);
        commentRepo = new CommentRepository(_perCtx);
        trophyRepo = new TrophyRepository(_perCtx);
        swiftlyRepo = new SwiftlyRepository(_perCtx);

        // initialize the swiftly invoker
        swiftlyInvoker = new Invoker("swiftly_invoker", omgr);
        swiftlyInvoker.setDaemon(true);
        swiftlyInvoker.start();

        // initialize the mail invoker
        mailInvoker = new Invoker("mail_invoker", omgr);
        mailInvoker.setDaemon(true);
        mailInvoker.start();
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
        return new PeeredDatabaseConfigRegistry(_perCtx, invoker, _peerMan);
    }

    @Override // from WhirledServer
    protected SceneRegistry createSceneRegistry ()
        throws Exception
    {
        return new MsoySceneRegistry(invmgr, _sceneRepo, _eventLog);
    }

    @Override // from CrowdServer
    protected BodyLocator createBodyLocator ()
    {
        return new BodyLocator() {
            public BodyObject get (Name visibleName) {
                return _online.get(((MemberName) visibleName).getMemberId());
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

    @Override // from MsoyBaseServer
    protected void finishInit ()
        throws Exception
    {
        super.finishInit();

        // initialize our authenticator
        author.init(_eventLog);

        // start up our peer manager
        log.info("Running in cluster mode as node '" + ServerConfig.nodeName + "'.");
        _peerMan.init(_perCtx, invoker, ServerConfig.nodeName, ServerConfig.sharedSecret,
                      ServerConfig.backChannelHost, ServerConfig.serverHost, getListenPorts()[0]);

        // intialize various services
        spotProv = new SpotProvider(omgr, plreg, screg);
        invmgr.registerDispatcher(new SpotDispatcher(spotProv), SpotCodes.WHIRLED_GROUP);
        _adminMan.init();
        memberMan.init(memberRepo, groupRepo);
        friendMan.init();
        mailMan.init(mailRepo, memberRepo, itemMan);
        channelMan.init(invmgr);
        _jabberMan.init();
        itemMan.init(_perCtx, _eventLog);
        swiftlyMan.init();
        petMan.init(invmgr);
        gameReg.init(itemMan.getGameRepository());
        supportMan.init(_perCtx);

        GameManager.setUserIdentifier(new GameManager.UserIdentifier() {
            public int getUserId (BodyObject bodyObj) {
                return ((MemberObject) bodyObj).getMemberId(); // will return 0 for guests
            }
        });
        DictionaryManager.init("data/dictionary");

        sceneRepo.init(itemMan.getDecorRepository());

        // create and start up our HTTP server
        httpServer = new MsoyHttpServer(_logdir, _eventLog);
        httpServer.start();

        // if we're a dev deployment and our policy port is not privileged, run the policy server
        // right in the msoy server to simplify life for developers
        if (DeploymentConfig.devDeployment && ServerConfig.socketPolicyPort > 1024 &&
                ServerConfig.nodeName.equals("msoy1")) {
            _policyServer = MsoyPolicyServer.init();
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
            _perCtx.initializeManagedRecords(true);
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
        if (lastModified <= _codeModified || _adminMan.statObj.serverRebootTime != 0L) {
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
        _adminMan.scheduleReboot(playersOnline ? 2 : 0, "codeUpdateAutoRestart");
    }

    /** Our runtime jabber manager. */
    @Inject protected JabberManager _jabberMan;

    /** Manages interactions with our peer servers. */
    @Inject protected MsoyPeerManager _peerMan;

    /** Manages our external game servers. */
    @Inject protected MsoyGameRegistry _gameReg;

    /** Our runtime swiftly editor manager. */
    @Inject protected SwiftlyManager _swiftlyMan;

    /** The Msoy scene repository. */
    @Inject protected MsoySceneRepository _sceneRepo;

    /** Our runtime admin manager. */
    @Inject protected MsoyAdminManager _adminMan;

    /** Used to auto-restart the development server when its code is updated. */
    protected long _codeModified;

    /** A policy server used on dev deployments. */
    protected IoAcceptor _policyServer;

    /** A mapping from member name to member object for all online members. */
    protected static HashIntMap<MemberObject> _online = new HashIntMap<MemberObject>();

    /** Check for modified code every 30 seconds. */
    protected static final long AUTO_RESTART_CHECK_INTERVAL = 30 * 1000L;
}
