//
// $Id$

package com.threerings.msoy.server;

import java.io.File;
import java.security.Security;
import java.util.Iterator;

import com.google.inject.Inject;
import com.google.inject.Injector;

import com.samskivert.jdbc.depot.PersistenceContext;

import com.threerings.util.MessageManager;

import com.threerings.presents.server.PresentsDObjectMgr;
import com.threerings.presents.server.ReportManager;
import com.threerings.bureau.server.BureauRegistry;
import com.threerings.crowd.server.PlaceManager;

import com.threerings.parlor.rating.server.persist.RatingRepository;
import com.threerings.stats.server.persist.StatRepository;

import com.threerings.admin.server.AdminProvider;
import com.threerings.admin.server.ConfigRegistry;
import com.threerings.whirled.server.WhirledServer;

import com.whirled.game.server.persist.GameCookieRepository;

import com.threerings.msoy.admin.server.RuntimeConfig;
import com.threerings.msoy.person.server.persist.FeedRepository;
import com.threerings.msoy.person.server.persist.ProfileRepository;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.world.server.persist.MemoryRepository;

import static com.threerings.msoy.Log.log;

/**
 * Provides the set of services that are shared between the Game and World servers.
 */
public abstract class MsoyBaseServer extends WhirledServer
{
    /** Configures dependencies needed by the Msoy servers. */
    public static class Module extends WhirledServer.Module
    {
        @Override protected void configure () {
            super.configure();
            bind(ReportManager.class).to(QuietReportManager.class);
            try {
                bind(PersistenceContext.class).toInstance(
                    new PersistenceContext("msoy", ServerConfig.createConnectionProvider(),
                                           ServerConfig.createCacheAdapter()));
            } catch (Exception e) {
                addError(e);
            }
        }
    }

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

    /** Contains information on our member profiles. */
    public static ProfileRepository profileRepo;

    /** The Msoy feed repository. */
    public static FeedRepository feedRepo;

    /** The container for our bureaus (server-side processes for user code). */
    public static BureauRegistry breg;

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

    @Override // from WhirledServer
    public void init (Injector injector)
        throws Exception
    {
        // before doing anything else, let's ensure that we don't cache DNS queries forever -- this
        // breaks Amazon S3, specifically.
        Security.setProperty("networkaddress.cache.ttl" , "30");

        // initialize event logger
        _eventLog.init(getIdent());

        // this one is needed by createAuthenticator() in our derived classes
        memberRepo = new MemberRepository(_perCtx, _eventLog);

        super.init(injector);

        // set up our default object access controller
        omgr.setDefaultAccessController(MsoyObjectAccess.DEFAULT);

        // create our various repositories
        ratingRepo = new RatingRepository(_perCtx);
        memoryRepo = new MemoryRepository(_perCtx);
        statRepo = new StatRepository(_perCtx);
        gameCookieRepo = new GameCookieRepository(_perCtx);
        profileRepo = new ProfileRepository(_perCtx);
        feedRepo = new FeedRepository(_perCtx);

        // create and set up our configuration registry and admin service
        confReg = createConfigRegistry();
        AdminProvider.init(invmgr, confReg);

        // create the bureau registry (subclasses will enable specific bureau types)
        breg = new BureauRegistry(
            "localhost:" + getListenPorts()[0], invmgr, omgr, invoker);

        // now initialize our runtime configuration, postponing the remaining server initialization
        // until our configuration objects are available
        RuntimeConfig.init(omgr, confReg);
        omgr.postRunnable(new PresentsDObjectMgr.LongRunnable () {
            public void run () {
                try {
                    finishInit();
                } catch (Exception e) {
                    log.warning("Server initialization failed.", e);
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
                log.warning("Place manager failed shutting down [where=" + pmgr.where() + "].", e);
            }
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
    protected void finishInit ()
        throws Exception
    {
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

    /** Provides database access to all of our repositories. */
    @Inject protected PersistenceContext _perCtx;

    /** Sends event information to an external log database. */
    @Inject protected MsoyEventLogger _eventLog;

    /** The directory that contains our log files. */
    protected static File _logdir = new File(ServerConfig.serverRoot, "log");
}
