//
// $Id$

package com.threerings.msoy.server;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import com.whirled.game.server.DictionaryManager;
import com.whirled.game.server.GameCookieManager;
import com.whirled.game.server.RepoCookieManager;
import com.whirled.game.server.persist.GameCookieRepository;

import org.apache.mina.common.IoAcceptor;

import com.samskivert.util.Interval;
import com.samskivert.util.Invoker;
import com.samskivert.util.Lifecycle;

import com.threerings.util.Name;

import com.threerings.presents.net.AuthRequest;
import com.threerings.presents.peer.server.EHCachePeerCoordinator;
import com.threerings.presents.peer.server.PeerManager;
import com.threerings.presents.server.Authenticator;
import com.threerings.presents.server.ClientManager;
import com.threerings.presents.server.ClientResolver;
import com.threerings.presents.server.PresentsSession;
import com.threerings.presents.server.SessionFactory;

import com.threerings.crowd.chat.server.ChatChannelManager;
import com.threerings.crowd.peer.server.CrowdPeerManager;
import com.threerings.crowd.server.BodyLocator;
import com.threerings.crowd.server.PlaceRegistry;

import com.threerings.admin.server.AdminManager;
import com.threerings.admin.server.ConfigRegistry;
import com.threerings.admin.server.PeeredDatabaseConfigRegistry;

import com.threerings.bureau.server.BureauRegistry;
import com.threerings.cron.server.CronLogic;

import com.threerings.parlor.game.data.UserIdentifier;
import com.threerings.parlor.server.ParlorManager;

import com.threerings.whirled.server.SceneRegistry;
import com.threerings.whirled.server.persist.SceneRepository;
import com.threerings.whirled.util.SceneFactory;

import com.threerings.messaging.DelayedMessageConnection;
import com.threerings.messaging.MessageConnection;
import com.threerings.messaging.amqp.AMQPMessageConfig;
import com.threerings.messaging.amqp.AMQPMessageConnection;
import com.threerings.pulse.server.DepotPulseRecorder;
import com.threerings.pulse.server.JVMPulseRecorder;
import com.threerings.pulse.server.PeerPulseRecorder;
import com.threerings.pulse.server.PresentsPulseRecorder;
import com.threerings.pulse.server.PulseModule;

import com.threerings.msoy.admin.server.MsoyAdminManager;
import com.threerings.msoy.admin.server.MsoyPulseRecorder;
import com.threerings.msoy.bureau.server.MsoyBureauRegistry;
import com.threerings.msoy.chat.server.JabberManager;
import com.threerings.msoy.chat.server.MsoyChatChannelManager;
import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.game.data.MsoyUserIdentifier;
import com.threerings.msoy.game.server.GameGameRegistry;
import com.threerings.msoy.game.server.WorldGameRegistry;
import com.threerings.msoy.game.server.persist.MsoyGameCookieRepository;
import com.threerings.msoy.item.server.ItemManager;
import com.threerings.msoy.money.server.BlingPoolDistributor;
import com.threerings.msoy.money.server.MoneyLogic;
import com.threerings.msoy.party.server.PartyRegistry;
import com.threerings.msoy.peer.server.MsoyPeerManager;
import com.threerings.msoy.room.server.MsoySceneFactory;
import com.threerings.msoy.room.server.MsoySceneRegistry;
import com.threerings.msoy.room.server.PetManager;
import com.threerings.msoy.room.server.SceneLogic;
import com.threerings.msoy.server.MsoyClientManager;
import com.threerings.msoy.server.persist.BatchInvoker;
import com.threerings.msoy.spam.server.SpamLogic;
import com.threerings.msoy.web.server.MsoyHttpServer;
import com.threerings.msoy.world.server.WorldManager;
import com.threerings.msoy.world.tour.server.TourManager;

import static com.threerings.msoy.Log.log;

/**
 * Brings together all of the services needed by the World server.
 */
@Singleton
public class MsoyServer extends MsoyBaseServer
{
    /** Configures dependencies needed by the world server. */
    public static class MsoyModule extends MsoyBaseModule
    {
        @Override protected void configure () {
            super.configure();

            // register the Pulse recorders
            install(new PulseModule(JVMPulseRecorder.class, DepotPulseRecorder.class,
                PresentsPulseRecorder.class, PeerPulseRecorder.class,
                MsoyPulseRecorder.class, MsoyHttpServer.class));

            // presents dependencies
            bind(Authenticator.class).to(MsoyAuthenticator.class);
            bind(ClientManager.class).to(MsoyClientManager.class);
            bind(PeerManager.class).to(MsoyPeerManager.class);
            bind(ConfigRegistry.class).to(PeeredDatabaseConfigRegistry.class);
            bind(AdminManager.class).to(MsoyAdminManager.class);
            bind(BureauRegistry.class).to(MsoyBureauRegistry.class);
            // crowd dependencies
            bind(BodyLocator.class).to(MemberLocator.class);
            bind(PlaceRegistry.class).to(RoomRegistry.class);
            bind(CrowdPeerManager.class).to(MsoyPeerManager.class);
            bind(ChatChannelManager.class).to(MsoyChatChannelManager.class);
            // vilya whirled dependencies
            bind(SceneRepository.class).to(SceneLogic.class);
            bind(SceneFactory.class).to(MsoySceneFactory.class);
            bind(SceneRegistry.class).to(MsoySceneRegistry.class);
            bind(SceneRegistry.ConfigFactory.class).to(MsoySceneFactory.class);
            // whirled game dependencies
            bind(GameCookieManager.class).to(RepoCookieManager.class);
            bind(GameCookieRepository.class).to(MsoyGameCookieRepository.class);
            // msoy auth dependencies
            bind(AuthenticationDomain.class).to(OOOAuthenticationDomain.class);
            // Messaging dependencies
            bind(MessageConnection.class).toInstance(createAMQPConnection());
            // cron logic dependencies
            bind(CronLogic.class).to(MsoyCronLogic.class);
        }
    }

    @Singleton
    public static class MsoyCronLogic extends CronLogic {
        @Inject public MsoyCronLogic (Lifecycle cycle, @BatchInvoker Invoker invoker) {
            super(cycle, invoker);
        }
    }

    /**
     * Starts everything a runnin'.
     */
    public static void main (final String[] args)
    {
        // don't let people not test
        if (-1 != DeploymentConfig.mediaURL.indexOf(DeploymentConfig.serverURL)) {
            System.err.println("Sorry, to properly test whirled your mediaURL needs to be " +
                "a different server than your serverURL. Luckily, the easy trick is to " +
                "change the mediaURL to be your IP address. These values can be editied in " +
                "etc/msoy-server.properties. You may need a static IP for your workstation.");
            System.exit(-1);
            return;
        }

        // configure the invoker with the correct warning threshold
        Invoker.setDefaultLongThreshold(ServerConfig.longInvokerUnit);

        runServer(new MsoyModule(), new PresentsServerModule(MsoyServer.class));
    }

    @Override // from MsoyBaseServer
    public void init (final Injector injector)
        throws Exception
    {
        super.init(injector);

        // we need to register to manually shutdown a couple of bits
        _lifecycle.addComponent(new Lifecycle.ShutdownComponent() {
            public void shutdown () {
                if (_policyServer != null) {
                    _policyServer.unbindAll();
                }
                try {
                    _messageConn.close();
                } catch (IOException ioe) {
                    log.warning("Failed to close the connection to the messaging server.", ioe);
                }
            }
        });

        // configure our default client factory
        _clmgr.setDefaultSessionFactory(new SessionFactory() {
            public Class<? extends PresentsSession> getSessionClass (final AuthRequest areq) {
                return MsoySession.class;
            }
            public Class<? extends ClientResolver> getClientResolverClass (final Name username) {
                return MsoyClientResolver.class;
            }
        });

        // initialize our HTTP server
        _httpServer.init(new File(ServerConfig.serverRoot, "log"));

        // start up our peer manager
        log.info("Running in cluster mode as node '" + ServerConfig.nodeName + "'.");
        _peerMan.init(_sceneReg, ServerConfig.nodeName, ServerConfig.sharedSecret,
                      ServerConfig.backChannelHost, ServerConfig.serverHost, getListenPorts()[0]);

        // give the EHCache coordinator access to our peering guts
        EHCachePeerCoordinator.initWithPeers(_peerMan);

        // intialize various services
        _adminMan.init(_invmgr, _cacheMgr);
        _memberMan.init();
        _friendMan.init();
        _itemMan.init();
        _wgameReg.init();
        _partyReg.init();
        _moneyLogic.init(_cacheMgr);

        // tell our dictionary manager where to find its dictionaries
        _dictMan.init("data/dictionary");

        // tell games how to identify our users
        UserIdentifier.setIder(new MsoyUserIdentifier());

        // TEMP: give a peer manager reference to MemberNodeActions
        MemberNodeActions.init(_peerMan);

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
            new AutoRestartChecker().schedule(AUTO_RESTART_CHECK_INTERVAL, true);
        }

        // kick off spam jobs
        _spamLogic.init();

        // and subscription mangement
        _subscripLogic.init();

        log.info("Msoy server initialized.");
    }

    /**
     * On dev deployments, restart the policy server, including these ports from another node
     * on this machine.
     */
    public void addPortsToPolicy (final int[] ports)
    {
        if (!DeploymentConfig.devDeployment || _lifecycle.isShuttingDown()) {
            return;
        }

        if (_otherNodePorts == null) {
            _otherNodePorts = Sets.newHashSet();
        }
        _otherNodePorts.addAll(Ints.asList(ports));

        if (_policyServer != null) {
            _policyServer.unbindAll();
        }
        try {
            _policyServer = MsoyPolicyServer.init(Ints.toArray(_otherNodePorts));
        } catch (final IOException ioe) {
            log.warning("Failed to restart MsoyPolicyServer with new ports", ioe);
        }
    }

    /**
     * On dev deployments, restarts the policy server, removing these ports from another node
     * on this machine.
     */
    public void removePortsFromPolicy (final int[] ports)
    {
        if (!DeploymentConfig.devDeployment || _lifecycle.isShuttingDown()) {
            return;
        }

        if (_otherNodePorts == null) {
            _otherNodePorts = Sets.newHashSet();
        }
        _otherNodePorts.remove(ports);

        if (_policyServer != null) {
            _policyServer.unbindAll();
        }
        try {
            _policyServer = MsoyPolicyServer.init(Ints.toArray(_otherNodePorts));
        } catch (final IOException ioe) {
            log.warning("Failed to restart MsoyPolicyServer with ports removed", ioe);
        }
    }

    @Override // from MsoyBaseServer
    protected String getIdent ()
    {
        return ServerConfig.nodeName;
    }

    @Override // from PresentsServer
    protected int[] getListenPorts ()
    {
        return ServerConfig.serverPorts;
    }

    /**
     * Creates a connection to the AMQP server based on the configuration settings.
     */
    protected static MessageConnection createAMQPConnection ()
    {
        DelayedMessageConnection delayedConn = new DelayedMessageConnection();
        AMQPMessageConfig config = ServerConfig.getAMQPMessageConfig();
        if (config == null) {
            log.info("No AMQP messaging server configured.");
        } else {
            delayedConn.init(new AMQPMessageConnection(config));
        }
        return delayedConn;
    }

    // Note well: this interval does not run on the dobj thread
    protected class AutoRestartChecker extends Interval
    {
        public AutoRestartChecker () {
            super(RUN_DIRECT);
            _codeModified = codeModifiedTime();
        }

        @Override public void expired () {
            final long lastModified = codeModifiedTime();
            if (lastModified <= _codeModified) {
                return;
            }
            _omgr.postRunnable(new Runnable() {
                public void run () {
                    _adminMan.scheduleReboot(0, "codeUpdateAutoRestart");
                }
            });
            cancel(); // we've scheduled a reboot, so we can stop this interval
        }

        @Override public String toString () {
            return "checkAutoRestart interval";
        }

        protected long codeModifiedTime () {
            return new File(ServerConfig.serverRoot, "dist/msoy-code.jar").lastModified();
        }

        protected long _codeModified;
    }

    /** A policy server used on dev deployments. */
    protected IoAcceptor _policyServer;

    /** On dev deployments, we keep track of the ports on other nodes (hosted on the same machine
     * that need to be accepted by the policy server. */
    protected Set<Integer> _otherNodePorts;

    /** Distributes Bling once a day; referenced here so it'll load. */
    @Inject protected BlingPoolDistributor _blingDistributor;

    /** Handles dictionary services for games. */
    @Inject protected DictionaryManager _dictMan;

    /** Handles management of member's friends lists. */
    @Inject protected FriendManager _friendMan;

    // these need to be injected here to ensure that they're created at server startup time rather
    // than lazily the first time they're referenced
    @Inject protected GameCookieManager _cookMan;

    /** Manages lobbies and other game bits on this server. */
    @Inject protected GameGameRegistry _ggameReg;

    /** Handles item-related services. */
    @Inject protected ItemManager _itemMan;

    /** Our runtime jabber manager. */
    @Inject protected JabberManager _jabberMan;

    /** Our runtime member manager. */
    @Inject protected MemberManager _memberMan;

    /** Connection to the AMQP messaging server. */
    @Inject protected MessageConnection _messageConn;

    /** Provides money services. */
    @Inject protected MoneyLogic _moneyLogic;

    /** Our runtime admin manager. */
    @Inject protected MsoyAdminManager _adminMan;

    /** Handles management of chat channels. */
    @Inject protected MsoyChatChannelManager _channelMan;

    /** Handles HTTP servlet requests. */
    @Inject protected MsoyHttpServer _httpServer;

    /** Our runtime msoy manager. */
    @Inject protected MsoyManager _msoyMan;

    /** Manages interactions with our peer servers. */
    @Inject protected MsoyPeerManager _peerMan;

    /** Keeps track of our locally resolved scenes and how to resolve them. */
    @Inject protected MsoySceneRegistry _sceneReg;

    /** Provides parlor game services. */
    @Inject protected ParlorManager _parlorMan;

    /** Manages our parties. */
    @Inject protected PartyRegistry _partyReg;

    /** Handles our cuddly little pets. */
    @Inject protected PetManager _petMan;

    /** The spam logic mail sender, it needs to be told to kick off its jobs. */
    @Inject protected SpamLogic _spamLogic;

    /** Manages subscriptions. */
    @Inject protected SubscriptionLogic _subscripLogic;

    /** The Whirled Tour manager. */
    @Inject protected TourManager _tourMan;

    /** Manages our external game servers. */
    @Inject protected WorldGameRegistry _wgameReg;

    /** The Whirled World manager. */
    @Inject protected WorldManager _worldMan;

    static {
        MemberName.isSpacePred = new Predicate<Character>() {
            public boolean apply (Character c) {
                return Character.isSpaceChar(c);
            }
        };
    }

    /** Check for modified code every 30 seconds. */
    protected static final long AUTO_RESTART_CHECK_INTERVAL = 30 * 1000L;
}
