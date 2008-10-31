//
// $Id$

package com.threerings.msoy.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.StringUtil;

import com.threerings.bureau.server.BureauAuthenticator;
import com.threerings.bureau.server.BureauRegistry;

import com.threerings.msoy.admin.gwt.BureauLauncherInfo;

import com.threerings.msoy.bureau.data.BureauLauncherClientObject;
import com.threerings.msoy.bureau.data.BureauLauncherCodes;
import com.threerings.msoy.bureau.server.BureauLauncherAuthenticator;
import com.threerings.msoy.bureau.server.BureauLauncherClientFactory;
import com.threerings.msoy.bureau.server.BureauLauncherDispatcher;
import com.threerings.msoy.bureau.server.BureauLauncherProvider;
import com.threerings.msoy.bureau.server.BureauLauncherSender;
import com.threerings.msoy.bureau.server.MsoyBureauClientFactory;

import com.threerings.presents.client.InvocationService.ResultListener;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.ObjectDeathListener;
import com.threerings.presents.dobj.ObjectDestroyedEvent;

import com.threerings.presents.server.ClientManager;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;
import com.threerings.presents.server.ShutdownManager;
import com.threerings.presents.server.net.ConnectionManager;

import com.whirled.bureau.data.BureauTypes;

import static com.threerings.msoy.Log.log;

/**
 * Singleton to maintain bureau launchers, launch new bureaus and deal with other bureau-related
 * tasks.
 */
@Singleton
public class BureauManager
{
    /**
     * Sets up the bureau manager.
     */
    public void init (int listenPort)
    {
        _listenPort = listenPort;
        
        // initialize the bureau registry
        _bureauReg.init();

        // configure some bureau related business
        if (ServerConfig.localBureaus) {
            // hook up thane as a local command
            log.info("Running thane bureaus locally");
            _bureauReg.setCommandGenerator(
                BureauTypes.THANE_BUREAU_TYPE, new ThaneCommandGenerator(), BUREAU_TIMEOUT);

        } else {
            // hook up bureau launching system for thane
            log.info("Running thane bureaus remotely");
            _bureauReg.setLauncher(
                BureauTypes.THANE_BUREAU_TYPE, new RemoteBureauLauncher(), BUREAU_TIMEOUT);
            _conmgr.addChainedAuthenticator(new BureauLauncherAuthenticator());
            _invmgr.registerDispatcher(new BureauLauncherDispatcher(new BureauLauncherProvider() {
                public void getGameServerRegistryOid (ClientObject caller, ResultListener arg1)
                    throws InvocationException {
                    arg1.requestProcessed(_gameServerRegistryOid);
                }

                public void launcherInitialized (ClientObject caller) {
                    BureauManager.this.launcherInitialized(caller);
                }

                public void setBureauLauncherInfo (ClientObject caller, BureauLauncherInfo arg1) {
                    BureauManager.this.setBureauLauncherInfo(caller, arg1);
                }
            }), BureauLauncherCodes.BUREAU_LAUNCHER_GROUP);

            ShutdownManager.Shutdowner killLaunchers = new ShutdownManager.Shutdowner() {
                public void shutdown () {
                    shutdownLaunchers();
                }
            };
            _shutmgr.registerShutdowner(killLaunchers);
        }
        _conmgr.addChainedAuthenticator(new BureauAuthenticator(_bureauReg));
    }

    /**
     * Configures the client factories for accepting bureau connections. Must be called after the
     * primary factory is set up.
     */
    public void configClientFactories ()
    {
        // now that our primary client factories are configured, we can register our chained bureau
        // factories which sit on top of whatever factory the server uses for normal clients
        _clmgr.setClientFactory(new MsoyBureauClientFactory(_clmgr.getClientFactory()));
        _clmgr.setClientFactory(new BureauLauncherClientFactory(_clmgr.getClientFactory()));
    }

    /**
     * Sets the object id that bureau launchers should subscribe to to obtain information about
     * game servers.
     */
    public void setGameServerRegistryOid (int oid)
    {
        _gameServerRegistryOid = oid;
    }
    
    /**
     * Sends out a request to all bureau launchers to resend their information.
     */
    public void refreshBureauLauncherInfo()
    {
        for (BureauLauncherClientObject clobj : _launcherClients.values()) {
            BureauLauncherSender.requestInfo(clobj, ServerConfig.serverHost, _listenPort);
        }
    }

    /**
     * Retrieves the current information on all bureau launchers.
     */
    public BureauLauncherInfo[] getBureauLauncherInfo()
    {
        BureauLauncherInfo[] infos = new BureauLauncherInfo[_launcherInfo.size()];
        int idx = 0;
        for (BureauLauncherInfo info : _launcherInfo.values()) {
            infos[idx++] = info;
        }
        return infos;
    }
    
    protected void launcherInitialized (final ClientObject launcher)
    {
        // this launcher is now available to take sender requests
        log.info("Launcher initialized", "client", launcher);
        _launcherClients.put(launcher.getOid(), (BureauLauncherClientObject)launcher);
        launcher.addListener(new ObjectDeathListener () {
            public void objectDestroyed (final ObjectDestroyedEvent event) {
                launcherDestroyed(event.getTargetOid());
            }
        });
    }

    protected void setBureauLauncherInfo (ClientObject caller, BureauLauncherInfo info)
    {
        BureauLauncherInfo previous = _launcherInfo.get(caller.getOid());
        info.version = previous == null ? 1 : previous.version + 1;
        _launcherInfo.put(caller.getOid(), info);
    }

    /** Selects a registered launcher for the next bureau. */
    protected BureauLauncherClientObject selectLauncher ()
    {
        // select one at random
        // TODO: select the one with the lowest current load. this should involve some measure
        // of the actual machine load since some bureaus may have more game instances than others
        // and some instances may produce more load than others.
        final int size = _launcherClients.size();
        final BureauLauncherClientObject[] launchers = new BureauLauncherClientObject[size];
        _launcherClients.values().toArray(launchers);
        return launchers[(new java.util.Random()).nextInt(size)];
    }

    /**
     * Called internally when a launcher connection is terminated. The specific launcher may no
     * longer be used to fulfill bureau requests.
     */
    protected void launcherDestroyed (final int oid)
    {
        log.info("Launcher destroyed", "oid", oid);
        _launcherInfo.remove(oid);
        _launcherClients.remove(oid);
    }

    /**
     * Tells all connected bureau launchers to shutdown.
     */
    protected void shutdownLaunchers ()
    {
        for (final ClientObject launcher : _launcherClients.values()) {
            log.info("Shutting down launcher", "launcher", launcher);
            BureauLauncherSender.shutdownLauncher(launcher);
        }
    }

    protected class ThaneCommandGenerator
        implements BureauRegistry.CommandGenerator
    {
        public String[] createCommand (
            final String bureauId,
            final String token) {
            final String windowToken = StringUtil.md5hex(ServerConfig.windowSharedSecret);
            return new String[] {
                ServerConfig.serverRoot + "/bin/runthaneclient", "msoy", bureauId, token,
                "localhost", String.valueOf(_listenPort), windowToken};
        }
    }
    
    protected class RemoteBureauLauncher
        implements BureauRegistry.Launcher
    {
        public void launchBureau (final String bureauId, final String token) {
            final BureauLauncherClientObject launcher = selectLauncher();
            log.info(
                "Launching bureau", "bureauId", bureauId, "who", launcher.who(), "hostname",
                launcher.hostname);
            BureauLauncherSender.launchThane(launcher, bureauId, token);
        }
    }
    
    /** The port that bureaus will connect back to. */
    protected int _listenPort;

    /** Currently logged in bureau launchers. */
    protected HashIntMap<BureauLauncherClientObject> _launcherClients =
        new HashIntMap<BureauLauncherClientObject>();
    
    /** Summary information about each bureau launcher. */
    protected HashIntMap<BureauLauncherInfo> _launcherInfo =
        new HashIntMap<BureauLauncherInfo>();
    
    protected int _gameServerRegistryOid;
    
    /** The container for our bureaus (server-side processes for user code). */
    @Inject protected BureauRegistry _bureauReg;

    /** The manager of network connections. */
    @Inject protected ConnectionManager _conmgr;

    /** The manager of clients. */
    @Inject protected ClientManager _clmgr;

    /** The manager of invocation services. */
    @Inject protected InvocationManager _invmgr;

    /** Handles orderly shutdown of our managers, etc. */
    @Inject protected ShutdownManager _shutmgr;
    
    /** Time to wait for bureaus to connect back. */
    protected static final int BUREAU_TIMEOUT = 30 * 1000;
}
