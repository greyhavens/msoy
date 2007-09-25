//
// $Id$

package com.threerings.msoy.server;

import java.io.File;
import java.security.Security;
import java.util.Iterator;
import java.util.logging.Level;

import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.StaticConnectionProvider;
import com.samskivert.jdbc.depot.CacheAdapter;
import com.samskivert.jdbc.depot.PersistenceContext;

import com.samskivert.util.AuditLogger;
import com.threerings.util.MessageManager;
import com.threerings.presents.server.PresentsDObjectMgr;

import com.threerings.crowd.server.PlaceManager;
import com.threerings.ezgame.server.persist.GameCookieRepository;

import com.threerings.parlor.rating.server.persist.RatingRepository;
import com.threerings.stats.server.persist.StatRepository;

import com.threerings.admin.server.AdminProvider;
import com.threerings.admin.server.ConfigRegistry;
import com.threerings.admin.server.DatabaseConfigRegistry;
import com.threerings.whirled.server.WhirledServer;

import com.threerings.msoy.admin.server.RuntimeConfig;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.world.server.persist.MemoryRepository;

import static com.threerings.msoy.Log.log;

/**
 * Provides the set of services that are shared between the Game and World servers.
 */
public abstract class MsoyBaseServer extends WhirledServer
{
    /** Provides database access to all of our repositories. */
    public static PersistenceContext perCtx;

    /** Provides a mechanism for translating strings on the server. <em>Note:</em> avoid using this
     * if at all possible. Delay translation to the client so that we can properly react to the
     * client's locale. Translating on the server means that we treat all clients as if they are
     * using the default locale of the server. */
    public static MessageManager msgMan = new MessageManager("rsrc.i18n");

    /** Maintains a registry of runtime configuration information. */
    public static ConfigRegistry confReg;

    /** Contains information on our members. */
    public static MemberRepository memberRepo;

    /** Contains the rating data for each player and game. */
    public static RatingRepository ratingRepo;

    /** Maintains "smart" digital item memories. */
    public static MemoryRepository memoryRepo;

    /** Manages the persistent repository of stats. */
    public static StatRepository statRepo;

    /** Stores user's game cookies. */
    public static GameCookieRepository gameCookieRepo;

    /** Sends event information to an external log database. */
    public static MsoyEventLogger eventLog;
    
    /**
     * Creates an audit log with the specified name (which should not include
     * the <code>.log</code> suffix) in our server log directory.
     */
    public static AuditLogger createAuditLog (String logname)
    {
        // qualify our log file with the nodename to avoid collisions
        if (ServerConfig.nodeName != null) {
            logname = logname + "_" + ServerConfig.nodeName;
        }
        return new AuditLogger(_logdir, logname + ".log");
    }

    /**
     * Logs a message to the general audit log.
     */
    public static void generalLog (String message)
    {
        _glog.log(message);
    }

    /**
     * Ensures that the calling thread is the distributed object event dispatch thread, throwing an
     * {@link IllegalStateException} if it is not.
     */
    public static void requireDObjThread ()
    {
        if (!omgr.isDispatchThread()) {
            String errmsg = "This method must be called on the distributed object thread.";
            throw new IllegalStateException(errmsg);
        }
    }

    /**
     * Ensures that the calling thread <em>is not</em> the distributed object event dispatch
     * thread, throwing an {@link IllegalStateException} if it is.
     */
    public static void refuseDObjThread ()
    {
        if (omgr.isDispatchThread()) {
            String errmsg = "This method must not be called on the distributed object thread.";
            throw new IllegalStateException(errmsg);
        }
    }

    @Override
    public void init ()
        throws Exception
    {
        // before doing anything else, let's ensure that we don't cache DNS queries forever -- this
        // breaks Amazon S3, specifically.
        Security.setProperty("networkaddress.cache.ttl" , "30");

        // create our JDBC bits before calling super.init() because our superclass will attempt to
        // create our authenticator and we need that ready by then
        _conProv = new StaticConnectionProvider(ServerConfig.getJDBCConfig());
        CacheAdapter cacher = ServerConfig.createCacheAdapter();
        perCtx = new PersistenceContext("msoy", _conProv, cacher);

        // this one is needed by createAuthenticator() in our derived classes
        memberRepo = new MemberRepository(perCtx);

        super.init();

        // set up our default object access controller
        omgr.setDefaultAccessController(MsoyObjectAccess.DEFAULT);

        // create our various repositories
        ratingRepo = new RatingRepository(perCtx);
        memoryRepo = new MemoryRepository(perCtx);
        statRepo = new StatRepository(perCtx);
        gameCookieRepo = new GameCookieRepository(perCtx);

        // create and set up our configuration registry and admin service
        confReg = createConfigRegistry();
        AdminProvider.init(invmgr, confReg);

        // initialize event logger
        eventLog = new MsoyEventLogger(ServerConfig.eventLogURL);
        
        // now initialize our runtime configuration, postponing the remaining server initialization
        // until our configuration objects are available
        RuntimeConfig.init(omgr, confReg);
        omgr.postRunnable(new PresentsDObjectMgr.LongRunnable () {
            public void run () {
                try {
                    finishInit();
                } catch (Exception e) {
                    log.log(Level.WARNING, "Server initialization failed.", e);
                    System.exit(-1);
                }
            }
        });
    }

    @Override
    public void shutdown ()
    {
        super.shutdown();

        // shut down all active games and rooms
        for (Iterator<PlaceManager> iter = plreg.enumeratePlaceManagers(); iter.hasNext(); ) {
            PlaceManager pmgr = iter.next();
            try {
                pmgr.shutdown();
            } catch (Exception e) {
                log.log(Level.WARNING, "Place manager failed shutting down " +
                        "[where=" + pmgr.where() + "].", e);
            }
        }
    }

    @Override // from PresentsServer
    protected void invokerDidShutdown ()
    {
        super.invokerDidShutdown();

        // shutdown our persistence context (cache, JDBC connections)
        perCtx.shutdown();

        // close our audit logs
        _glog.close();
    }

    /**
     * Called once our runtime configuration information is loaded and ready.
     */
    protected void finishInit ()
        throws Exception
    {
    }

    /**
     * Creates the admin config registry for use by this server.
     */
    protected abstract ConfigRegistry createConfigRegistry ()
        throws Exception;

    /** The connection provider used to access our JDBC databases. Don't use this; rather use
     * {@link #perCtx}. */
    protected static ConnectionProvider _conProv;

    /** The directory that contains our log files. */
    protected static File _logdir = new File(ServerConfig.serverRoot, "log");

    /** Our general audit log. */
    protected static AuditLogger _glog = createAuditLog("server");
}
