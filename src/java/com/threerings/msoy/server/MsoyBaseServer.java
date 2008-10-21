//
// $Id$

package com.threerings.msoy.server;

import java.security.Security;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.matcher.Matchers;

import net.sf.ehcache.CacheManager;

import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.depot.EHCacheAdapter;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.StringUtil;

import com.whirled.bureau.data.BureauTypes;
import com.whirled.game.server.DictionaryManager;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.ObjectDeathListener;
import com.threerings.presents.dobj.ObjectDestroyedEvent;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.PresentsDObjectMgr;
import com.threerings.presents.server.ReportManager;
import com.threerings.presents.server.ShutdownManager;

import com.threerings.admin.server.AdminProvider;
import com.threerings.admin.server.ConfigRegistry;

import com.threerings.whirled.server.WhirledServer;

import com.threerings.bureau.server.BureauAuthenticator;
import com.threerings.bureau.server.BureauRegistry;

import com.threerings.msoy.data.StatType;
import com.threerings.msoy.server.util.Retry;
import com.threerings.msoy.server.util.RetryInterceptor;

import com.threerings.msoy.admin.server.RuntimeConfig;

import com.threerings.msoy.bureau.data.BureauLauncherClientObject;
import com.threerings.msoy.bureau.data.BureauLauncherCodes;
import com.threerings.msoy.bureau.server.BureauLauncherAuthenticator;
import com.threerings.msoy.bureau.server.BureauLauncherClientFactory;
import com.threerings.msoy.bureau.server.BureauLauncherDispatcher;
import com.threerings.msoy.bureau.server.BureauLauncherProvider;
import com.threerings.msoy.bureau.server.BureauLauncherSender;
import com.threerings.msoy.bureau.server.MsoyBureauClientFactory;

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
            // depot dependencies (we will initialize this persistence context later when the
            // server is ready to do database operations; not initializing it now ensures that no
            // one sneaks any database manipulations into the dependency resolution phase)
            bind(PersistenceContext.class).toInstance(new PersistenceContext());
            // presents dependencies
            bind(ReportManager.class).to(QuietReportManager.class);
            // msoy dependencies
            bind(CacheManager.class).toInstance(CacheManager.getInstance());
            bindInterceptor(Matchers.any(), Matchers.annotatedWith(Retry.class),
                            new RetryInterceptor());
        }
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

        // initialize our persistence context
        ConnectionProvider conprov = ServerConfig.createConnectionProvider();
        _perCtx.init("msoy", conprov, new EHCacheAdapter(_cacheMgr));

        // initialize our depot repositories; running all of our schema and data migrations
        _perCtx.initializeRepositories(true);

        // set up our default object access controller
        _omgr.setDefaultAccessController(MsoyObjectAccess.DEFAULT);

        // create and set up our configuration registry, admin service and runtime config
        final ConfigRegistry confReg = createConfigRegistry();
        AdminProvider.init(_invmgr, confReg);
        _runtime.init(_omgr, confReg);

        // initialize the bureau registry
        _bureauReg.init();

        // initialize our dictionary services
        _dictMan.init("data/dictionary");

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
            _invmgr.registerDispatcher(new BureauLauncherDispatcher(this),
                BureauLauncherCodes.BUREAU_LAUNCHER_GROUP);

            _shutmgr.registerShutdowner(new ShutdownManager.Shutdowner() {
                public void shutdown () {
                    shutdownLaunchers();
                }
            });
        }
        _conmgr.addChainedAuthenticator(new BureauAuthenticator(_bureauReg));

        // set up the right client factories
        configClientFactory();

        // now that our primary client factories are configured, we can register our chained bureau
        // factories which sit on top of whatever factory the server uses for normal clients
        _clmgr.setClientFactory(new MsoyBureauClientFactory(_clmgr.getClientFactory()));
        _clmgr.setClientFactory(new BureauLauncherClientFactory(_clmgr.getClientFactory()));
    }

    // from BureauLauncherProvider
    public void launcherInitialized (final ClientObject launcher)
    {
        // this launcher is now available to take sender requests
        log.info("Launcher initialized", "client", launcher);
        _launchers.put(launcher.getOid(), (BureauLauncherClientObject)launcher);
        launcher.addListener(new ObjectDeathListener () {
            public void objectDestroyed (final ObjectDestroyedEvent event) {
                launcherDestroyed(event.getTargetOid());
            }
        });
    }

    // from BureauLauncherProvider
    public void getGameServerRegistryOid (final ClientObject caller,
                                          final InvocationService.ResultListener arg1)
        throws InvocationException
    {
        arg1.requestProcessed(0);
    }

    /**
     * Derived classes need to override this and configure their main client factory.
     */
    protected abstract void configClientFactory ();

    /**
     * Called internally when a launcher connection is terminated. The specific launcher may no
     * longer be used to fulfill bureau requests.
     */
    protected void launcherDestroyed (final int oid)
    {
        log.info("Launcher destroyed", "oid", oid);
        _launchers.remove(oid);
    }

    /**
     * Tells all connected bureau launchers to shutdown.
     */
    protected void shutdownLaunchers ()
    {
        for (final ClientObject launcher : _launchers.values()) {
            log.info("Shutting down launcher", "launcher", launcher);
            BureauLauncherSender.shutdownLauncher(launcher);
        }
    }

    @Override // from PresentsServer
    protected void invokerDidShutdown ()
    {
        super.invokerDidShutdown();

        // shutdown our persistence context (JDBC connections) and the cache manager
        _perCtx.shutdown();
        _cacheMgr.shutdown();

        // and shutdown our event logger now that everything else is done shutting down
        _eventLog.shutdown();
    }

    /** Selects a registered launcher for the next bureau. */
    protected BureauLauncherClientObject selectLauncher ()
    {
        // select one at random
        // TODO: select the one with the lowest current load. this should involve some measure
        // of the actual machine load since some bureaus may have more game instances than others
        // and some instances may produce more load than others.
        final int size = _launchers.size();
        final BureauLauncherClientObject[] launchers = new BureauLauncherClientObject[size];
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
        @Override protected void logReport (final String report) {
            // TODO: nix this and publish this info via JMX
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
                "localhost", String.valueOf(getListenPorts()[0]), windowToken};
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

    /** Currently logged in bureau launchers. */
    protected HashIntMap<BureauLauncherClientObject> _launchers =
        new HashIntMap<BureauLauncherClientObject>();

    /** Used for caching things. */
    @Inject protected CacheManager _cacheMgr;

    /** Provides database access to all of our repositories. */
    @Inject protected PersistenceContext _perCtx;

    /** Maintains runtime modifiable configuration information. */
    @Inject protected RuntimeConfig _runtime;

    /** Sends event information to an external log database. */
    @Inject protected MsoyEventLogger _eventLog;

    /** The container for our bureaus (server-side processes for user code). */
    @Inject protected BureauRegistry _bureauReg;

    /** Handles dictionary services for games. */
    @Inject protected DictionaryManager _dictMan;

    /** This is needed to ensure that the StatType enum's static initializer runs before anything
     * else in the server that might rely on stats runs. */
    protected static final StatType STAT_TRIGGER = StatType.UNUSED;

    /** Time to wait for bureaus to connect back. */
    protected static final int BUREAU_TIMEOUT = 30 * 1000;
}
