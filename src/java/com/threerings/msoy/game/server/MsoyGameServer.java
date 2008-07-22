//
// $Id$

package com.threerings.msoy.game.server;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import com.threerings.util.Name;

import com.threerings.presents.net.AuthRequest;
import com.threerings.presents.server.Authenticator;
import com.threerings.presents.server.ClientFactory;
import com.threerings.presents.server.ClientResolver;
import com.threerings.presents.server.PresentsClient;
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

import com.whirled.game.server.GameCookieManager;
import com.whirled.game.server.RepoCookieManager;


import com.threerings.msoy.server.MsoyBaseServer;


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
            bind(PlaceRegistry.class).to(GamePlaceRegistry.class);
            bind(Authenticator.class).to(MsoyGameAuthenticator.class);
            bind(GameCookieManager.class).to(RepoCookieManager.class);
            bind(BodyLocator.class).to(PlayerLocator.class);
            bind(PresentsServer.class).to(MsoyGameServer.class);
        }
    }

    /** Manages lobbies and other game bits on this server. */
    public static GameGameRegistry gameReg;

    /** Handles sandboxed game server code. */
    public static HostedGameManager hostedMan = new HostedGameManager();

    /** Manages our connection back to our parent world server. */
    public static WorldServerClient worldClient;

    /**
     * Returns true if this server is running, false if not.
     */
    public static boolean isActive ()
    {
        return (memberRepo != null);
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

        // TEMP: initialize our legacy static members
        gameReg = _gameReg;
        worldClient = _worldClient;

        // set up the right client factory
        _clmgr.setClientFactory(new ClientFactory() {
            public Class<? extends PresentsClient> getClientClass (AuthRequest areq) {
                return MsoyGameClient.class;
            }
            public Class<? extends ClientResolver> getClientResolverClass (Name username) {
                return MsoyGameClientResolver.class;
            }
        });

        GameManager.setUserIdentifier(new GameManager.UserIdentifier() {
            public int getUserId (BodyObject bodyObj) {
                return ((PlayerObject) bodyObj).getMemberId(); // will return 0 for guests
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
        return new DatabaseConfigRegistry(_perCtx, invoker);
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
            ClassLoader loader = hostedMan.getClassLoader(config);
            if (loader == null) {
                return super.createPlaceManager(config);
            }
            return (PlaceManager)Class.forName(
                config.getManagerClassName(), true, loader).newInstance();
        }
    }

    /** Manages lobbies and other game bits on this server. */
    @Inject protected GameGameRegistry _gameReg;

    /** Provides the game-side API for watching member movements on the world server. */
    @Inject protected GameWatcherManager _watchMan;
    
    /** Provides parlor game services. */
    @Inject protected ParlorManager _parlorMan;

    /** Manages our connection back to our parent world server. */
    @Inject protected WorldServerClient _worldClient;

    /** The port on which we listen for client connections. */
    protected int _listenPort;

    /** The port on which we connect back to our parent server. */
    protected int _connectPort;
}
