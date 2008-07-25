//
// $Id$

package com.threerings.msoy.server;

import java.io.File;
import java.security.Security;

import com.google.inject.Inject;
import com.google.inject.Injector;

import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.depot.CacheAdapter;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.Invoker;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.ObjectDeathListener;
import com.threerings.presents.dobj.ObjectDestroyedEvent;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;
import com.threerings.presents.server.PresentsDObjectMgr;
import com.threerings.presents.server.ReportManager;
import com.threerings.presents.server.ShutdownManager;
import com.threerings.bureau.server.BureauRegistry;
import com.threerings.bureau.server.BureauAuthenticator;

import com.threerings.admin.server.AdminProvider;
import com.threerings.admin.server.ConfigRegistry;
import com.threerings.whirled.server.WhirledServer;

import com.whirled.bureau.data.BureauTypes;
import com.whirled.game.server.DictionaryManager;

import com.threerings.msoy.admin.server.RuntimeConfig;
import com.threerings.msoy.bureau.data.BureauLauncherCodes;
import com.threerings.msoy.bureau.server.BureauLauncherAuthenticator;
import com.threerings.msoy.bureau.server.BureauLauncherClientFactory;
import com.threerings.msoy.bureau.server.BureauLauncherDispatcher;
import com.threerings.msoy.bureau.server.BureauLauncherProvider;
import com.threerings.msoy.bureau.server.BureauLauncherSender;

import static com.threerings.msoy.Log.log;

/**
 * Provides the set of services that are shared between the Game and World servers.
 */
public abstract class MsoyBaseServer extends WhirledServer
    implements BureauLauncherProvider
{
    /** Configures dependencies needed by the Msoy servers. */
    public static class Module extends WhirledServer.Module
    {
        @Override protected void configure () {
            super.configure();
            try {
                _conprov = ServerConfig.createConnectionProvider();
                _cacher = ServerConfig.createCacheAdapter();
            } catch (Exception e) {
                addError(e);
            }
            // depot dependencies
            bind(PersistenceContext.class).toInstance(
                new PersistenceContext("msoy", _conprov, _cacher));
            // presents dependencies
            bind(ReportManager.class).to(QuietReportManager.class);
            // msoy dependencies
        }

        protected ConnectionProvider _conprov;
        protected CacheAdapter _cacher;
    }

    @Override // from WhirledServer
    public void init (final Injector injector)
        throws Exception
    {
        // before doing anything else, let's ensure that we don't cache DNS queries forever -- this
        // breaks Amazon S3, specifically.
        Security.setProperty("networkaddress.cache.ttl" , "30");

        // initialize event logger
        _eventLog.init(getIdent());

        super.init(injector);

        // set up our default object access controller
        _omgr.setDefaultAccessController(MsoyObjectAccess.DEFAULT);

        // create and set up our configuration registry and admin service
        ConfigRegistry confReg = createConfigRegistry();
        AdminProvider.init(_invmgr, confReg);

        // initialize the bureau registry (subclasses will enable specific bureau types)
        _bureauReg.init();

        // initialize our dictionary services
        _dictMan.init("data/dictionary");

        // now initialize our runtime configuration, postponing the remaining server initialization
        // until our configuration objects are available
        RuntimeConfig.init(_omgr, confReg);
        _omgr.postRunnable(new PresentsDObjectMgr.LongRunnable () {
            public void run () {
                try {
                    finishInit(injector);
                } catch (Exception e) {
                    log.warning("Server initialization failed.", e);
                    System.exit(-1);
                }
            }
        });

        if (ServerConfig.localBureaus) {
            // hook up thane as a local command
            log.info("Running thane bureaus locally");
            _bureauReg.setCommandGenerator(
                BureauTypes.THANE_BUREAU_TYPE, new ThaneCommandGenerator());

        } else {
            // hook up bureau launching system for thane
            log.info("Running thane bureaus remotely");
            _bureauReg.setLauncher(
                BureauTypes.THANE_BUREAU_TYPE, new RemoteBureauLauncher());
            _conmgr.addChainedAuthenticator(new BureauLauncherAuthenticator());
            _invmgr.registerDispatcher(new BureauLauncherDispatcher(this),
                BureauLauncherCodes.BUREAU_LAUNCHER_GROUP);

            _shutmgr.registerShutdowner(new ShutdownManager.Shutdowner() {
                public void shutdown () {
                    shutdownLaunchers();
                }
            });
        }

        _conmgr.addChainedAuthenticator(new BureauAuthenticator(_bureauReg));
    }

    // from BureauLauncherProvider
    public void launcherInitialized (ClientObject launcher)
    {
        // this launcher is now available to take sender requests
        log.info("Launcher initialized", "client", launcher);
        _launchers.put(launcher.getOid(), launcher);
        launcher.addListener(new ObjectDeathListener () {
            public void objectDestroyed (ObjectDestroyedEvent event) {
                launcherDestroyed(event.getTargetOid());
            }
        });
    }

    // from BureauLauncherProvider
    public void getGameServerRegistryOid (ClientObject caller,
                                          InvocationService.ResultListener arg1)
        throws InvocationException
    {
        arg1.requestProcessed(0);
    }

    /**
     * Called internally when a launcher connection is terminated. The specific launcher may no
     * longer be used to fulfill bureau requests.
     */
    protected void launcherDestroyed (int oid)
    {
        log.info("Launcher destroyed", "oid", oid);
        _launchers.remove(oid);
    }

    /**
     * Tells all connected bureau launchers to shutdown.
     */
    protected void shutdownLaunchers ()
    {
        for (ClientObject launcher : _launchers.values()) {
            log.info("Shutting down launcher", "launcher", launcher);
            BureauLauncherSender.shutdownLauncher(launcher);
        }
    }

    @Override // from PresentsServer
    protected void invokerDidShutdown ()
    {
        super.invokerDidShutdown();

        // shutdown our persistence context (cache, JDBC connections)
        _perCtx.shutdown();
    }

    /**
     * Called once our runtime configuration information is loaded and ready.
     */
    protected void finishInit (Injector injector)
        throws Exception
    {
        // prepare for bureau launcher connections
        _clmgr.setClientFactory(
            new BureauLauncherClientFactory(_clmgr.getClientFactory()));
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

    /**
     * Returns an identifier used to distinguish this server from others on this machine when
     * generating log files.
     */
    protected abstract String getIdent ();

    /**
     * Creates the admin config registry for use by this server.
     */
    protected abstract ConfigRegistry createConfigRegistry ()
        throws Exception;

    /** Disables state of the server report logging. */
    protected static class QuietReportManager extends ReportManager
    {
        @Override protected void logReport (String report) {
            // TODO: nix this and publish this info via JMX
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
                "msoy", bureauId, token, "localhost",
                String.valueOf(getListenPorts()[0])};
        }
    }

    protected class RemoteBureauLauncher
        implements BureauRegistry.Launcher
    {
        public void launchBureau (String bureauId, String token) {
            ClientObject launcher = selectLauncher();
            BureauLauncherSender.launchThane(launcher, bureauId, token);
        }
    }

    /** Provides database access to all of our repositories. */
    @Inject protected PersistenceContext _perCtx;

    /** Sends event information to an external log database. */
    @Inject protected MsoyEventLogger _eventLog;

    /** The container for our bureaus (server-side processes for user code). */
    @Inject protected BureauRegistry _bureauReg;

    /** Handles dictionary services for games. */
    @Inject protected DictionaryManager _dictMan;

    /** Currently logged in bureau launchers. */
    protected HashIntMap<ClientObject> _launchers = new HashIntMap<ClientObject>();
}
