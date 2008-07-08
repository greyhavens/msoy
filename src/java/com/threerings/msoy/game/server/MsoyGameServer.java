//
// $Id$

package com.threerings.msoy.game.server;

import java.util.Map;

import com.google.common.collect.Maps;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.LoggingLogProvider;
import com.samskivert.util.OneLineLogFormatter;

import com.threerings.util.Name;

import com.threerings.presents.annotation.EventThread;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.ObjectDeathListener;
import com.threerings.presents.dobj.ObjectDestroyedEvent;
import com.threerings.presents.net.AuthRequest;
import com.threerings.presents.server.Authenticator;
import com.threerings.presents.server.ClientFactory;
import com.threerings.presents.server.ClientResolver;
import com.threerings.presents.server.PresentsClient;
import com.threerings.presents.server.ShutdownManager;

import com.threerings.admin.server.ConfigRegistry;
import com.threerings.admin.server.DatabaseConfigRegistry;

import com.threerings.bureau.data.BureauCredentials;
import com.threerings.bureau.server.BureauRegistry;
import com.threerings.bureau.server.BureauAuthenticator;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.server.BodyLocator;
import com.threerings.crowd.server.PlaceManager;
import com.threerings.crowd.server.PlaceRegistry;

import com.threerings.whirled.server.SceneRegistry;

import com.threerings.parlor.game.server.GameManager;
import com.threerings.parlor.server.ParlorManager;

import com.whirled.game.server.GameCookieManager;
import com.whirled.game.server.RepoCookieManager;

import com.whirled.game.server.WhirledGameManager;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.MsoyBaseServer;
import com.threerings.msoy.server.ServerConfig;

import com.threerings.msoy.item.server.persist.AvatarRepository;

import com.threerings.msoy.game.data.PlayerObject;
import com.threerings.msoy.bureau.data.BureauLauncherCodes;

import static com.threerings.msoy.Log.log;

/**
 * A server that does nothing but host games.
 */
public class MsoyGameServer extends MsoyBaseServer
    implements BureauLauncherProvider
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

        // prepare for bureau launcher clients
        _clmgr.setClientFactory(
            new BureauLauncherClientFactory(_clmgr.getClientFactory()));

        GameManager.setUserIdentifier(new GameManager.UserIdentifier() {
            public int getUserId (BodyObject bodyObj) {
                return ((PlayerObject) bodyObj).getMemberId(); // will return 0 for guests
            }
        });

        // connect back to our parent world server
        _worldClient.init(_listenPort, _connectPort);

        if (ServerConfig.localBureaus) {
            // hook up thane as a local command
            log.info("Running thane bureaus locally");
            _bureauReg.setCommandGenerator(
                WhirledGameManager.THANE_BUREAU, new ThaneCommandGenerator());

        } else {
            // hook up bureau launching system for thane
            log.info("Running thane bureaus remotely");
            _bureauReg.setLauncher(
                WhirledGameManager.THANE_BUREAU, new RemoteBureauLauncher());
            _conmgr.addChainedAuthenticator(new BureauLauncherAuthenticator());
            _invmgr.registerDispatcher(new BureauLauncherDispatcher(this),
                BureauLauncherCodes.BUREAU_LAUNCHER_GROUP);
        }
        _conmgr.addChainedAuthenticator(new BureauAuthenticator(_bureauReg));

        log.info("Game server initialized.");
    }

    // from BureauLauncherProvider
    public void launcherInitialized (ClientObject launcher)
    {
        // this launcher is now available to take sender requests
        // TODO: notify our world server that we are now a bureau-enabled game server
        log.info("Launcher initialized", "client", launcher);
        _launchers.put(launcher.getOid(), launcher);
        launcher.addListener(new ObjectDeathListener () {
            public void objectDestroyed (ObjectDestroyedEvent event) {
                launcherDestroyed(event.getTargetOid());
            }
        });
    }

    /**
     * Called internally when a launcher connection is terminated. The specific launcher may no
     * longer be used to fulfill bureau requests.
     */
    protected void launcherDestroyed (int oid)
    {
        // TODO: if this is the last launcher, notify our world server
        log.info("Launcher destroyed", "oid", oid);
        _launchers.remove(oid);
    }

    /** Selects a registered launcher for the next bureau. */
    protected ClientObject selectLauncher ()
    {
        // select one at random
        // TODO: select the one with the lowest current load. this should involve some measure
        // of the actual machine load since some bureaus may have more game instances than others
        // and some instances may produce more load than others.
        int size = _launchers.size();
        ClientObject[] launchers = new ClientObject[size];
        _launchers.values().toArray(launchers);
        return launchers[(new java.util.Random()).nextInt(size)];
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

    protected class ThaneCommandGenerator
        implements BureauRegistry.CommandGenerator
    {
        public String[] createCommand (
            String bureauId,
            String token) {
            return new String[] {
                ServerConfig.serverRoot + "/bin/runthaneclient",
                bureauId, token, "localhost", 
                String.valueOf(getListenPorts()[0])};
        }
    }

    protected class RemoteBureauLauncher
        implements BureauRegistry.Launcher
    {
        public void launchBureau (
            String bureauId,
            String token) {
            ClientObject launcher = selectLauncher();
            BureauLauncherSender.launchThane(
                launcher, bureauId, token, ServerConfig.serverHost, 
                getListenPorts()[0]);
        }
    }

    /** Manages lobbies and other game bits on this server. */
    @Inject protected GameGameRegistry _gameReg;

    /** Provides parlor game services. */
    @Inject protected ParlorManager _parlorMan;

    /** Manages our connection back to our parent world server. */
    @Inject protected WorldServerClient _worldClient;

    /** The port on which we listen for client connections. */
    protected int _listenPort;

    /** The port on which we connect back to our parent server. */
    protected int _connectPort;

    protected HashIntMap<ClientObject> _launchers = 
        new HashIntMap<ClientObject>();
}
