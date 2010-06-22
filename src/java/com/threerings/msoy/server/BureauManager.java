//
// $Id$

package com.threerings.msoy.server;

import java.io.IOException;
import java.util.Map;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.Interval;
import com.samskivert.util.Lifecycle;
import com.samskivert.util.ResultListener;
import com.samskivert.util.StringUtil;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.ObjectDeathListener;
import com.threerings.presents.dobj.ObjectDestroyedEvent;
import com.threerings.presents.server.ClientManager;
import com.threerings.presents.server.ClientResolver;
import com.threerings.presents.server.InvocationManager;
import com.threerings.presents.server.PresentsDObjectMgr;
import com.threerings.presents.server.net.ConnectionManager;

import com.threerings.bureau.server.BureauRegistry;

import com.threerings.msoy.admin.gwt.BureauLauncherInfo;

import com.threerings.msoy.bureau.data.BureauLauncherClientObject;
import com.threerings.msoy.bureau.data.BureauLauncherCodes;
import com.threerings.msoy.bureau.server.BureauLauncherDispatcher;
import com.threerings.msoy.bureau.server.BureauLauncherProvider;
import com.threerings.msoy.bureau.server.BureauLauncherSender;

import com.whirled.bureau.data.BureauTypes;

import static com.threerings.msoy.Log.log;

/**
 * Singleton to maintain bureau launchers, launch new bureaus and deal with other bureau-related
 * tasks.
 */
@Singleton
public class BureauManager
    implements Lifecycle.ShutdownComponent
{
    /**
     * Exception to be thrown if the server is configured to launch bureaus remotely and a bureau
     * launch is attempted before any remote launchers are connected.
     */
    public static class LauncherNotConnected extends IOException
    {
        public LauncherNotConnected () {
            super("No bureau launchers connected");
        }
    }

    @Inject public BureauManager (Lifecycle cycle, ClientManager clmgr)
    {
        // make sure we are shut down before the client manager so our shutdown message can be sent
        // prior to our client getting shutdown
        cycle.addComponent(this);
        cycle.addShutdownConstraint(this, Lifecycle.Constraint.RUNS_BEFORE, clmgr);
    }

    /**
     * Sets up the bureau manager.
     */
    public void init (int listenPort)
    {
        _listenPort = listenPort;

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
            _invmgr.registerDispatcher(new BureauLauncherDispatcher(new BureauLauncherProvider() {
                public void launcherInitialized (ClientObject caller) {
                    BureauManager.this.launcherInitialized(caller);
                }
                public void setBureauLauncherInfo (
                    ClientObject caller, BureauLauncherInfo info) {
                    BureauManager.this.setBureauLauncherInfo(caller, info);
                }
            }), BureauLauncherCodes.BUREAU_LAUNCHER_GROUP);
        }
    }

    /**
     * Retrieves the current information on all bureau launchers.
     */
    public void getBureauLauncherInfo (final ResultListener<BureauLauncherInfo[]> listener)
    {
        // only handle one request at a time on this server
        if (_launcherInfo != null) {
            listener.requestFailed(new Exception("e.request_already_pending"));
        }

        // reset the results buffer and wait for it to fill up, up to 5 seconds
        _launcherInfo = Maps.newHashMap();
        new Interval(_omgr) {
            public void expired () {
                if (_launcherInfo.size() == _launcherClients.size()) {
                    finish();
                }
                else if (++_count > 10) {
                    finish();
                }
            }

            public void finish () {
                cancel();
                BureauLauncherInfo[] infos = new BureauLauncherInfo[_launcherClients.size()];
                int idx = 0;
                for (BureauLauncherClientObject clobj : _launcherClients.values()) {
                    // fill in with an error message if the launcher didn't respond
                    BureauLauncherInfo info = _launcherInfo.get(clobj.getOid());
                    if (info == null) {
                        info = new BureauLauncherInfo();
                        info.bureaus = new BureauLauncherInfo.BureauInfo[0];
                        info.connections = new String[0];
                        info.error = "e.not_responding";
                        info.hostname = clobj.hostname;
                    }
                    infos[idx++] = info;
                }
                _launcherInfo = null;
                listener.requestCompleted(infos);
            }
            protected int _count;
        }.schedule(500, true);

        // request that the info be sent
        for (BureauLauncherClientObject clobj : _launcherClients.values()) {
            BureauLauncherSender.requestInfo(clobj);
        }
    }

    // from Lifecycle.ShutdownComponent
    public void shutdown ()
    {
        for (final ClientObject launcher : _launcherClients.values()) {
            log.info("Shutting down launcher", "launcher", launcher.who());
            BureauLauncherSender.shutdownLauncher(launcher);
        }
    }

    protected void launcherInitialized (final ClientObject launcher)
    {
        // this launcher is now available to take sender requests
        log.info("Launcher initialized", "client", launcher.who());
        _launcherClients.put(launcher.getOid(), (BureauLauncherClientObject)launcher);
        launcher.addListener(new ObjectDeathListener () {
            public void objectDestroyed (final ObjectDestroyedEvent event) {
                launcherDestroyed(event.getTargetOid());
            }
        });
    }

    protected void setBureauLauncherInfo (ClientObject caller, BureauLauncherInfo info)
    {
        if (_launcherInfo == null) {
            log.warning("Launcher responding late", "caller", caller.username, "info", info);
            return;
        }

        BureauLauncherInfo previous = _launcherInfo.get(caller.getOid());
        if (previous != null) {
            log.warning("Launcher responding twice", "caller", caller.username, "info", info);
        }
        _launcherInfo.put(caller.getOid(), info);
    }

    /** Selects a registered launcher for the next bureau. */
    protected BureauLauncherClientObject selectLauncher ()
        throws LauncherNotConnected
    {
        // select one at random
        // TODO: select the one with the lowest current load. this should involve some measure
        // of the actual machine load since some bureaus may have more game instances than others
        // and some instances may produce more load than others.
        final int size = _launcherClients.size();
        if (size == 0) {
            throw new LauncherNotConnected();
        }
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
        _launcherClients.remove(oid);
        if (_launcherInfo != null) {
            _launcherInfo.remove(oid);
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
        public void launchBureau (final String bureauId, final String token)
            throws LauncherNotConnected {
            final BureauLauncherClientObject launcher = selectLauncher();
            log.info("Launching bureau", "bureauId", bureauId, "who", launcher.who(),
                     "hostname", launcher.hostname);
            BureauLauncherSender.launchThane(launcher, bureauId, token);
        }
    }

    protected static class Resolver extends ClientResolver
    {
        public ClientObject createClientObject () {
            return new BureauLauncherClientObject();
        }
    }

    /** The port that bureaus will connect back to. */
    protected int _listenPort;

    /** Currently logged in bureau launchers. */
    protected Map<Integer, BureauLauncherClientObject> _launcherClients = Maps.newHashMap();

    /** Summary information about each bureau launcher. This is only set during an info request. */
    protected Map<Integer, BureauLauncherInfo> _launcherInfo;

    // dependencies
    @Inject protected BureauRegistry _bureauReg;
    @Inject protected ClientManager _clmgr;
    @Inject protected ConnectionManager _conmgr;
    @Inject protected InvocationManager _invmgr;
    @Inject protected PresentsDObjectMgr _omgr;

    /** Time to wait for bureaus to connect back. */
    protected static final int BUREAU_TIMEOUT = 30 * 1000;
}
