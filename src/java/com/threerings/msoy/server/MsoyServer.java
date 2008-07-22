//
// $Id$

package com.threerings.msoy.server;

import java.io.File;
import java.util.Iterator;

import org.apache.mina.common.IoAcceptor;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import com.samskivert.jdbc.depot.PersistenceContext;

import com.samskivert.servlet.user.UserRepository;
import com.samskivert.util.Interval;
import com.samskivert.util.Invoker;

import com.threerings.util.Name;

import com.threerings.presents.peer.server.PeerManager;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.net.AuthRequest;
import com.threerings.presents.server.Authenticator;
import com.threerings.presents.server.ClientFactory;
import com.threerings.presents.server.ClientResolver;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.PresentsClient;
import com.threerings.presents.server.PresentsDObjectMgr;
import com.threerings.presents.server.ShutdownManager;
import com.threerings.presents.client.InvocationService;

import com.threerings.admin.server.ConfigRegistry;
import com.threerings.admin.server.PeeredDatabaseConfigRegistry;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.server.BodyLocator;
import com.threerings.parlor.game.server.GameManager;

import com.threerings.whirled.server.SceneRegistry;
import com.threerings.whirled.server.persist.SceneRepository;
import com.threerings.whirled.util.SceneFactory;

import com.threerings.msoy.admin.server.MsoyAdminManager;
import com.threerings.msoy.chat.server.ChatChannelManager;
import com.threerings.msoy.chat.server.JabberManager;
import com.threerings.msoy.game.server.MsoyGameRegistry;
import com.threerings.msoy.game.server.persist.TrophyRepository;
import com.threerings.msoy.item.server.ItemManager;
import com.threerings.msoy.notify.server.NotificationManager;
import com.threerings.msoy.peer.server.MsoyPeerManager;
import com.threerings.msoy.server.persist.OOODatabase;
import com.threerings.msoy.swiftly.server.SwiftlyManager;
import com.threerings.msoy.web.client.DeploymentConfig;
import com.threerings.msoy.web.server.MsoyHttpServer;

import com.threerings.msoy.fora.server.persist.CommentRepository;
import com.threerings.msoy.fora.server.persist.ForumRepository;
import com.threerings.msoy.fora.server.persist.IssueRepository;
import com.threerings.msoy.group.server.persist.GroupRepository;
import com.threerings.msoy.person.server.persist.MailRepository;
import com.threerings.msoy.badge.server.BadgeManager;
import com.threerings.msoy.badge.server.persist.BadgeRepository;
import com.threerings.msoy.underwire.server.MsoyUnderwireManager;

import com.threerings.msoy.world.server.MsoySceneFactory;
import com.threerings.msoy.world.server.MsoySceneRegistry;
import com.threerings.msoy.world.server.PetManager;
import com.threerings.msoy.world.server.WorldWatcherManager;
import com.threerings.msoy.world.server.persist.MsoySceneRepository;

import com.threerings.msoy.bureau.server.WindowAuthenticator;
import com.threerings.msoy.bureau.server.WindowClientFactory;

import com.threerings.msoy.data.MemberObject;

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
            bind(Authenticator.class).to(MsoyAuthenticator.class);
            bind(MsoyAuthenticator.Domain.class).to(OOOAuthenticationDomain.class);
            bind(PersistenceContext.class).annotatedWith(OOODatabase.class).toInstance(
                new PersistenceContext(UserRepository.USER_REPOSITORY_IDENT, _conprov, _cacher));
            bind(PeerManager.class).to(MsoyPeerManager.class);
            bind(SceneRepository.class).to(MsoySceneRepository.class);
            bind(SceneRegistry.class).to(MsoySceneRegistry.class);
            bind(SceneFactory.class).to(MsoySceneFactory.class);
            bind(SceneRegistry.ConfigFactory.class).to(MsoySceneFactory.class);
            bind(BodyLocator.class).to(MemberLocator.class);
            bind(PresentsServer.class).to(MsoyServer.class);
        }
    }

    /** All blocking Swiftly subversion actions must occur on this thread. */
    public static Invoker swiftlyInvoker;

    /** An invoker for sending email. */
    public static Invoker mailInvoker;

    /** Our runtime admin manager. */
    public static MsoyAdminManager adminMan = new MsoyAdminManager();

    /** Manages interactions with our peer servers. */
    public static MsoyPeerManager peerMan;

    /** Our runtime member manager. */
    public static MemberManager memberMan;

    /** Handles management of member's friends lists. */
    public static FriendManager friendMan;

    /** Our runtime chat channel manager. */
    public static ChatChannelManager channelMan;

    /** Our runtime support manager. */
    public static MsoyUnderwireManager supportMan;

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

    /** Provides access to the badge repository. */
    public static BadgeRepository badgeRepo;

    /** Handles the updating of a member's badges. */
    public static BadgeManager badgeMan;

    /** The Msoy scene repository. */
    public static MsoySceneRepository sceneRepo;

    /** The Msoy item manager. */
    public static ItemManager itemMan;

    /** Our runtime swiftly editor manager. */
    public static SwiftlyManager swiftlyMan;

    /** Manages our external game servers. */
    public static MsoyGameRegistry gameReg;

    /** Handles our cuddly little pets. */
    public static PetManager petMan;

    /** Handles notifications to clients. */
    public static NotificationManager notifyMan;

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
        if (ServerConfig.autoRestart) {
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
            _httpServer.stop();
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
        adminMan = _adminMan;
        itemMan = _itemMan;
        channelMan = _channelMan;
        memberMan = _memberMan;
        friendMan = _friendMan;
        petMan = _petMan;
        notifyMan = _notifyMan;
        supportMan = _supportMan;
        sceneRepo = _sceneRepo;
        mailRepo = _mailRepo;
        groupRepo = _groupRepo;
        forumRepo = _forumRepo;
        issueRepo = _issueRepo;
        commentRepo = _commentRepo;
        trophyRepo = _trophyRepo;
        badgeRepo = _badgeRepo;
        badgeMan = _badgeMan;

        // we need to know when we're shutting down
        _shutmgr.registerShutdowner(this);

        // set up the right client factory
        _clmgr.setClientFactory(new ClientFactory() {
            public Class<? extends PresentsClient> getClientClass (AuthRequest areq) {
                return MsoyClient.class;
            }
            public Class<? extends ClientResolver> getClientResolverClass (Name username) {
                return MsoyClientResolver.class;
            }
        });

        // Add in the authenticator and client factory which will allow bureau windows (for avrgs)
        // to be distinguished and connected
        _conmgr.addChainedAuthenticator(new WindowAuthenticator());
        _clmgr.setClientFactory(new WindowClientFactory(_clmgr.getClientFactory()));

        // initialize the swiftly invoker
        swiftlyInvoker = new Invoker("swiftly_invoker", _omgr);
        swiftlyInvoker.setDaemon(true);
        swiftlyInvoker.start();

        // initialize the mail invoker
        mailInvoker = new Invoker("mail_invoker", _omgr);
        mailInvoker.setDaemon(true);
        mailInvoker.start();

        // initialize our HTTP server
        _httpServer.init(injector, _logdir);
    }

    @Override // from BureauLauncherProvider
    public void getGameServerRegistryOid (
        ClientObject caller, InvocationService.ResultListener arg1)
        throws InvocationException
    {
        int oid = _gameReg.getServerRegistryObject().getOid();
        arg1.requestProcessed(oid);
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
        return new PeeredDatabaseConfigRegistry(_perCtx, _invoker, _peerMan);
    }

    @Override // from PresentsServer
    protected int[] getListenPorts ()
    {
        return ServerConfig.serverPorts;
    }

    @Override // from MsoyBaseServer
    protected void finishInit (Injector injector)
        throws Exception
    {
        super.finishInit(injector);

        // start up our peer manager
        log.info("Running in cluster mode as node '" + ServerConfig.nodeName + "'.");
        _peerMan.init(injector, ServerConfig.nodeName, ServerConfig.sharedSecret,
                      ServerConfig.backChannelHost, ServerConfig.serverHost, getListenPorts()[0]);

        // intialize various services
        _adminMan.init();
        _memberMan.init();
        _friendMan.init();
        _channelMan.init(_invmgr);
        _jabberMan.init();
        _itemMan.init();
        swiftlyMan.init(_invmgr);
        _petMan.init(injector);
        _gameReg.init(itemMan.getGameRepository());
        _supportMan.init(_perCtx);

        GameManager.setUserIdentifier(new GameManager.UserIdentifier() {
            public int getUserId (BodyObject bodyObj) {
                return ((MemberObject) bodyObj).getMemberId(); // will return 0 for guests
            }
        });

        sceneRepo.init(itemMan.getDecorRepository());

        // start up our HTTP server
        _httpServer.start();

        // if we're a dev deployment and our policy port is not privileged, run the policy server
        // right in the msoy server to simplify life for developers
        if (DeploymentConfig.devDeployment && ServerConfig.socketPolicyPort > 1024 &&
                ServerConfig.nodeName.equals("msoy1")) {
            _policyServer = MsoyPolicyServer.init();
        }

        // start up an interval that checks to see if our code has changed and auto-restarts the
        // server as soon as possible when it has
        if (ServerConfig.autoRestart) {
            _codeModified = codeModifiedTime();
            new Interval() { // Note well: this interval does not run on the dobj thread
                public void expired () {
                    // ...we simply post a LongRunnable to do the job
                    _omgr.postRunnable(new PresentsDObjectMgr.LongRunnable() {
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
            _userCtx.initializeManagedRecords(true);
        }

        log.info("Msoy server initialized.");
    }

    @Override // from PresentsServer
    protected void invokerDidShutdown ()
    {
        super.invokerDidShutdown();

        // shutdown our persistence context (cache, JDBC connections)
        _userCtx.shutdown();
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
        for (Iterator<ClientObject> iter = _clmgr.enumerateClientObjects(); iter.hasNext(); ) {
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

    /** Handles HTTP servlet requests. */
    @Inject protected MsoyHttpServer _httpServer;

    /** Our runtime member manager. */
    @Inject protected MemberManager _memberMan;

    /** Handles management of member's friends lists. */
    @Inject protected FriendManager _friendMan;

    /** Handles item-related services. */
    @Inject protected ItemManager _itemMan;

    /** Handles chat channel-related services. */
    @Inject protected ChatChannelManager _channelMan;

    /** Handles our cuddly little pets. */
    @Inject protected PetManager _petMan;

    /** Handles notifications to clients. */
    @Inject protected NotificationManager _notifyMan;

    /** Our runtime support manager. */
    @Inject protected MsoyUnderwireManager _supportMan;

    /** The member movement observation manager. */
    @Inject protected WorldWatcherManager _watcherMan;

    /** Provides database access to the user databases. TODO: This should probably be removed. */
    @Inject protected @OOODatabase PersistenceContext _userCtx;

    /** Contains information on our groups. */
    @Inject protected GroupRepository _groupRepo;

    /** Contains information on our mail system. */
    @Inject protected MailRepository _mailRepo;

    /** Contains information on our forums. */
    @Inject protected ForumRepository _forumRepo;

    /** Contains information on our issues. */
    @Inject protected IssueRepository _issueRepo;

    /** Contains member comments on various things. */
    @Inject protected CommentRepository _commentRepo;

    /** Provides access to our trophy metadata. */
    @Inject protected TrophyRepository _trophyRepo;

    /** Provides access to the badge repository. */
    @Inject protected BadgeRepository _badgeRepo;

    /** Handles updating a member's badges. */
    @Inject protected BadgeManager _badgeMan;

    /** Used to auto-restart the development server when its code is updated. */
    protected long _codeModified;

    /** A policy server used on dev deployments. */
    protected IoAcceptor _policyServer;

    /** Check for modified code every 30 seconds. */
    protected static final long AUTO_RESTART_CHECK_INTERVAL = 30 * 1000L;
}
