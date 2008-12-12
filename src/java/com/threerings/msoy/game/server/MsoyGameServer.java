//
// $Id$

package com.threerings.msoy.game.server;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import com.samskivert.util.RunQueue;
import com.threerings.util.Name;

import com.threerings.presents.net.AuthRequest;
import com.threerings.presents.server.Authenticator;
import com.threerings.presents.server.ReportManager;
import com.threerings.presents.server.SessionFactory;
import com.threerings.presents.server.ClientResolver;
import com.threerings.presents.server.PresentsSession;
import com.threerings.presents.server.PresentsServer;
import com.threerings.presents.server.ShutdownManager;

import com.threerings.admin.server.ConfigRegistry;
import com.threerings.admin.server.DatabaseConfigRegistry;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.server.BodyLocator;
import com.threerings.crowd.server.PlaceManager;
import com.threerings.crowd.server.PlaceRegistry;

import com.threerings.parlor.game.server.GameManager;
import com.threerings.parlor.server.ParlorManager;

import com.threerings.messaging.DelayedMessageConnection;
import com.threerings.messaging.MessageConnection;

import com.whirled.game.server.DictionaryManager;
import com.whirled.game.server.GameCookieManager;
import com.whirled.game.server.RepoCookieManager;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.MsoyBaseServer;
import com.threerings.msoy.server.ServerConfig;

import com.threerings.msoy.person.server.persist.ProfileRepository;

import com.threerings.msoy.game.data.PlayerObject;

import static com.threerings.msoy.Log.log;

/**
 * A server that does nothing but host games.
 */
public class MsoyGameServer extends MsoyBaseServer
{
    /** Configures dependencies needed by the world server. */
    public static class Module extends MsoyBaseServer.Module
    {
        @Override protected void configure () {
            super.configure();
            // presents dependencies
            bind(Authenticator.class).to(MsoyGameAuthenticator.class);
            bind(PresentsServer.class).to(MsoyGameServer.class);
            bind(ReportManager.class).toInstance(new ReportManager() {
                @Override public void init (RunQueue rqueue) {
                    // disable state of the server report logging by not calling super
                }
            });
            // crowd dependencies
            bind(BodyLocator.class).to(PlayerLocator.class);
            bind(PlaceRegistry.class).to(GamePlaceRegistry.class);
            // vilya game dependencies
            bind(GameCookieManager.class).to(RepoCookieManager.class);
            // The game server has no message connection, though it's needed by the money service
            bind(MessageConnection.class).toInstance(new DelayedMessageConnection());
        }
    }

    /**
     * Starts everything a runnin'.
     */
    public static void main (String[] args)
    {
        if (args.length < 2) {
            System.err.println("Usage: MsoyGameServer listenPort connectPort");
            System.exit(-1);
        }

        // TEMP: enable PING debugging on the dev server
        if (ServerConfig.autoRestart) {
            System.setProperty("ping_debug", "true");
        }

        Injector injector = Guice.createInjector(new Module());
        MsoyGameServer server = injector.getInstance(MsoyGameServer.class);
        try {
            server._listenPort = Integer.parseInt(args[0]);
            server._connectPort = Integer.parseInt(args[1]);
            server.init(injector);
            server.run();

        } catch (Exception e) {
            log.warning("Unable to initialize server", e);
            System.exit(255);
        }
    }

    @Override
    public void init (Injector injector)
        throws Exception
    {
        super.init(injector);

        // give our game game registry Injection Power (tm)
        _gameReg.init(injector);

        // tell our dictionary manager where to find its dictionaries
        _dictMan.init("data/dictionary");

        // tell GameManager how to identify our users
        GameManager.setUserIdentifier(new GameManager.UserIdentifier() {
            public int getUserId (BodyObject bodyObj) {
                int memberId = ((PlayerObject) bodyObj).getMemberId();
                return MemberName.isGuest(memberId) ? 0 : memberId;
            }
        });

        // connect back to our parent world server
        _worldClient.init(_listenPort, _connectPort);

        log.info("Game server initialized.");
    }

    @Override // from MsoyBaseServer
    protected String getIdent ()
    {
        return "p" + _listenPort;
    }

    @Override // from MsoyBaseServer
    protected ConfigRegistry createConfigRegistry ()
        throws Exception
    {
        // TODO: the game servers probably need to hear about changes to runtime config bits
        return _configReg;
    }

    @Override // from MsoyBaseServer
    protected void configSessionFactory ()
    {
        // set up the right client factory
        _clmgr.setSessionFactory(new SessionFactory() {
            public Class<? extends PresentsSession> getSessionClass (AuthRequest areq) {
                return MsoyGameSession.class;
            }
            public Class<? extends ClientResolver> getClientResolverClass (Name username) {
                return MsoyGameClientResolver.class;
            }
        });
    }

    @Override // from PresentsServer
    protected int[] getListenPorts ()
    {
        return new int[] { _listenPort };
    }

    @Singleton
    protected static class GamePlaceRegistry extends PlaceRegistry
    {
        @Inject public GamePlaceRegistry (ShutdownManager shutmgr) {
            super(shutmgr);
        }
        @Override protected PlaceManager createPlaceManager (PlaceConfig config) throws Exception {
            ClassLoader loader = _hostedMan.getClassLoader(config);
            if (loader == null) {
                return super.createPlaceManager(config);
            }
            return (PlaceManager)Class.forName(
                config.getManagerClassName(), true, loader).newInstance();
        }
        @Inject protected HostedGameManager _hostedMan;
    }

    /** Manages lobbies and other game bits on this server. */
    @Inject protected GameGameRegistry _gameReg;

    /** Provides parlor game services. */
    @Inject protected ParlorManager _parlorMan;

    /** Manages our connection back to our parent world server. */
    @Inject protected WorldServerClient _worldClient;

    /** Handles our runtime configuration stuffs. */
    @Inject protected DatabaseConfigRegistry _configReg;

    /** Handles dictionary services for games. */
    @Inject protected DictionaryManager _dictMan;

    // these need to be injected here to ensure that they're created at server startup time rather
    // than lazily the first time they're referenced
    @Inject protected ProfileRepository _profileRepo;
    @Inject protected GameCookieManager _cookMan;

    /** The port on which we listen for client connections. */
    protected int _listenPort;

    /** The port on which we connect back to our parent server. */
    protected int _connectPort;
}
