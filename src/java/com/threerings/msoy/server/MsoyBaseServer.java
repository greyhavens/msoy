//
// $Id$

package com.threerings.msoy.server;

import java.security.Security;

import com.google.inject.Inject;
import com.google.inject.Injector;

import net.sf.ehcache.CacheManager;

import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.util.Invoker;
import com.samskivert.depot.OldEHCacheAdapter;
import com.samskivert.depot.PersistenceContext;

import com.threerings.presents.server.ReportManager;
import com.threerings.presents.server.ShutdownManager;

import com.threerings.admin.server.AdminProvider;
import com.threerings.admin.server.ConfigRegistry;

import com.threerings.whirled.server.WhirledServer;

import com.threerings.msoy.data.StatType;
import com.threerings.msoy.server.persist.BatchInvoker;

import com.threerings.msoy.admin.server.RuntimeConfig;

/**
 * Provides the set of services that are shared between the Game and World servers.
 */
public abstract class MsoyBaseServer extends WhirledServer
    implements ShutdownManager.Shutdowner
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

            // bind the batch invoker
            bind(Invoker.class).annotatedWith(BatchInvoker.class).to(MsoyBatchInvoker.class);
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
        
        // we need to know when we're shutting down
        _shutmgr.registerShutdowner(this);

        // start the batch invoker thread
        _batchInvoker.start();

        // initialize our persistence context
        ConnectionProvider conprov = ServerConfig.createConnectionProvider();
        _perCtx.init("msoy", conprov, new OldEHCacheAdapter(_cacheMgr));

        // initialize our depot repositories; running all of our schema and data migrations
        _perCtx.initializeRepositories(true);

        // set up our default object access controller
        _omgr.setDefaultAccessController(MsoyObjectAccess.DEFAULT);

        // create and set up our configuration registry, admin service and runtime config
        final ConfigRegistry confReg = createConfigRegistry();
        AdminProvider.init(_invmgr, confReg);
        _runtime.init(_omgr, confReg);

        // initialize our bureau manager
        _bureauMgr.init(getListenPorts()[0]);

        // set up the right client factories
        configSessionFactory();

        _bureauMgr.configClientFactories();
    }

    /**
     * Derived classes need to override this and configure their main client factory.
     */
    protected abstract void configSessionFactory ();

    // from interface ShutdownManager.Shutdowner
    public void shutdown ()
    {
        // queue up a 'shutdown' unit on the batch invoker, after which it will shuttle
        // further units onto the main invoker instead
        _batchInvoker.shutdown();
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

    /** Used for caching things. */
    protected CacheManager _cacheMgr = CacheManager.getInstance();

    /** Provides database access to all of our repositories. */
    @Inject protected PersistenceContext _perCtx;

    /** Maintains runtime modifiable configuration information. */
    @Inject protected RuntimeConfig _runtime;

    /** Sends event information to an external log database. */
    @Inject protected MsoyEventLogger _eventLog;

    /** Handles state of the server reporting (used by the /status servlet). */
    @Inject protected ReportManager _reportMan;

    /** Manages our bureau launchers. */
    @Inject protected BureauManager _bureauMgr;

    /** The batch invoker thread. */
    @Inject protected MsoyBatchInvoker _batchInvoker;
    
    /** This is needed to ensure that the StatType enum's static initializer runs before anything
     * else in the server that might rely on stats runs. */
    protected static final StatType STAT_TRIGGER = StatType.UNUSED;
}
