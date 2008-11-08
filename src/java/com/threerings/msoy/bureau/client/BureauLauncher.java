//
// $Id$

package com.threerings.msoy.bureau.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.google.common.collect.Maps;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.util.Interval;
import com.samskivert.util.Logger;
import com.samskivert.util.RunQueue;
import com.samskivert.util.StringUtil;

import com.threerings.presents.server.SunSignalHandler;
import com.threerings.presents.server.ShutdownManager;
import com.threerings.presents.server.NativeSignalHandler;
import com.threerings.presents.annotation.EventQueue;
import com.threerings.presents.peer.server.persist.NodeRecord;
import com.threerings.presents.peer.server.persist.NodeRepository;

import com.threerings.bureau.util.BureauLogRedirector;

import com.threerings.msoy.admin.gwt.BureauLauncherInfo;
import com.threerings.msoy.bureau.server.BureauLauncherConfig;

import static com.threerings.msoy.Log.log;

/**
 * Operates a bureau launcher client for an msoy server.
 */
public class BureauLauncher
    implements ShutdownManager.Shutdowner
{
    /** Guice module for bureau launcher. */
    public static class Module extends AbstractModule
    {
        @Override protected void configure () {
            ConnectionProvider provider = null;
            try {
                provider = BureauLauncherConfig.createConnectionProvider();
            } catch (Exception e) {
                addError(e);
            }

            bind(PersistenceContext.class).toInstance(
                new PersistenceContext("msoy", provider, null));
            bind(RunQueue.class).annotatedWith(EventQueue.class).toInstance(_runner);
            bind(Runner.class).toInstance(_runner);
        }

        @EventQueue
        protected Runner _runner = new Runner();
    }

    /**
     * Basic queue of runnables.
     */
    public static class Runner implements RunQueue
    {
        /**
         * Execute until {@link #stop} is called and the queue is empty.
         */
        public void run ()
        {
            while (true) {
                if (!_queue.isEmpty()) {
                    Runnable r = _queue.remove(0);
                    try {
                        r.run();
                    } catch (Throwable t) {
                        log.warning("Error executing run queue item", "runnable", r, t);
                    }

                } else if (_stopped) {
                    break;

                } else {
                    try {
                        synchronized (this) {
                            wait();
                        }
                    } catch (InterruptedException ie) {
                        continue;
                    }
                }
            }
        }

        /**
         * Stop the run method once the queue has been depleted.
         */
        synchronized public void stop ()
        {
            _stopped = true;
            notify();
        }

        // from RunQueue
        synchronized public void postRunnable (Runnable r)
        {
            _queue.add(r);
            notify();
        }

        // from RunQueue
        public boolean isDispatchThread ()
        {
            return Thread.currentThread() == _dispatcher;
        }

        // from RunQueue
        public boolean isRunning ()
        {
            return !_stopped;
        }

        protected Thread _dispatcher = Thread.currentThread();
        protected List<Runnable> _queue = Collections.synchronizedList(new LinkedList<Runnable>());
        protected boolean _stopped;
    }

    /**
     * Runs the bureau launcher program. The arguments are not used and all data is read from
     * burl-server.properties. The launcher operates as follows:<br/>
     * <li>On startup and at intervals, connects to all world servers read from the
     * <code>NODES</code> table that are not already connected.</li>
     * <li>If there are 0 nodes in the node table, exits.</li>
     * <li>When a world server is successfully connected, subscribes to the game server registry
     * and connects to each server in the registry (as well as ones added later) that is not
     * already connected.</li>
     * <li>When {@link BureauLauncherConfig#worldServerWillAutoRestart} is set and any logoff
     * occurs, enters a faster polling mode to try and make sure we exit, thereby picking up new
     * code.</li>
     * @see NodeRepository
     * @see #PEER_POLL_INTERVAL
     * @see #clientLoggedOff
     */
    public static void main (String[] args)
    {
        Injector injector = Guice.createInjector(new Module());

        // register SIGTERM, SIGINT (ctrl-c) and a SIGHUP handlers
        boolean registered = false;
        try {
            registered = injector.getInstance(SunSignalHandler.class).init();
        } catch (Throwable t) {
            log.warning("Unable to register signal handlers", t);
        }
        if (!registered) {
            injector.getInstance(NativeSignalHandler.class).init();
        }

        BureauLauncher launcher = injector.getInstance(BureauLauncher.class);
        launcher.run();
    }

    /**
     * Creates a new bureau launcher.
     */
    @Inject public BureauLauncher (ShutdownManager shutmgr)
    {
        shutmgr.registerShutdowner(this);
    }

    /**
     * Runs the bureau launcher.
     */
    public void run ()
    {
        _pollNodes.schedule(0, PEER_POLL_INTERVAL);
        
        // reset our log limit when the file is rolled
        BureauFileAppender.setRollObserver(new BureauFileAppender.RollObserver() {
            public void logRolled(BureauFileAppender instance) {
                _runner.postRunnable(new Runnable() {
                    public void run() {
                        BureauLauncher.this.logRolled();
                    }
                });
            }
        });

        new Thread() {
            public void run () {
                _dbrunner.run();
            }
        }.start();

        // Print a status summary every 30 minutes (configurable)
        int summaryInterval = BureauLauncherConfig.summaryIntervalMillis;
        new Interval(_runner) {
            public void expired () {
                printSummary();
            }
        }.schedule(summaryInterval, summaryInterval);

        log.info("Starting");
        _runner.run();
        log.info("Exiting");
    }

    /**
     * Accesses the run queue held by the launcher.
     */
    public RunQueue getRunner ()
    {
        return _runner;
    }

    // from BureauLauncherReceiver
    public void launchThane (String bureauId, String token, String server, int port)
    {
        Bureau bureau = _bureaus.get(bureauId);
        if (bureau != null && bureau.isRunning()) {
            log.warning(
                "Did not expect to launch two bureaus with the same id", "bureauId", bureauId);
            return;
        }

        if (bureau == null) {
            _bureaus.put(bureauId, bureau = new Bureau(bureauId));
        }

        bureau.launch(server, port, token);
        ++_totalLaunched;
    }

    // from BureauLauncherReceiver
    public void shutdownLauncher ()
    {
        log.info("Received shutdown message");
        shutdown();
    }

    // from BureauLauncherReceiver
    public void requestInfo (String hostname, int port)
    {
        BureauLauncherInfo info = new BureauLauncherInfo();
        info.hostname = BureauLauncherConfig.serverHost;
        info.bureaus = new BureauLauncherInfo.BureauInfo[_bureaus.size()];
        info.connections = new String[0]; // TODO
        int idx = 0;
        for (Bureau bureau : _bureaus.values()) {
            info.bureaus[idx++] = bureau.getInfo();
        }
        Connections.Entry entry = _connections._clients.get(hostname + ":" + port);
        if (entry != null) {
            entry._client._service.setBureauLauncherInfo(entry._client, info);
        } else {
            log.info("Client not found", "hostname", hostname);
        }
    }
    
    // from ShutdownManager.Shutdowner
    public void shutdown ()
    {
        log.info("Shutting down bureau launcher");
        _connections.shutdown();
        _pollNodes.cancel();
        _runner.stop();
        _dbrunner.stop();
    }

    /**
     * Notify that a client has just logged off.
     */
    public void clientLoggedOff ()
    {
        // auto restart is not reliable in that the world server does not always call the
        // shutdownLauncher method presumably due to class loader issues. In order to make the
        // development server load new bureau launcher goodness, use the nodes table as a backup
        // to make sure we get shut down. We just increase the frequency of the node poll so that
        // the main servers do not stop and restart before we get a chance to see that the number
        // of entries has gone to zero.
        if (BureauLauncherConfig.worldServerWillAutoRestart) {
            startHyperPoll();
            _lastLogoffTime = System.currentTimeMillis();
        }
    }

    /**
     * Start polling the nodes table much more frequently and automatically return to normal
     * after a while.
     */
    protected void startHyperPoll ()
    {
        if (_lastLogoffTime == 0) {
            _pollNodes.schedule(0, HYPER_PEER_POLL_INTERVAL);
            new Interval(_runner) {
                public void expired () {
                    if (resumeNormalPoll()) {
                        cancel();
                    }
                }
            }.schedule(0, 500);
        }
    }

    /**
     * Return to normal polling frequency if it is time, return true if we did.
     */
    protected boolean resumeNormalPoll ()
    {
        if (System.currentTimeMillis() - _lastLogoffTime > RESUME_NORMAL_INTERVAL) {
            _pollNodes.schedule(PEER_POLL_INTERVAL, true);
            _lastLogoffTime = 0;
            return true;
        }
        return false;
    }

    /**
     * Reads from the node repository and posts a job to connect to each server.
     */
    protected void pollForNewHosts ()
    {
        try {
            final java.util.Collection<NodeRecord> nodes = _nodeRepo.loadNodes();

            _runner.postRunnable(new Runnable() {
                public void run() {
                    for (NodeRecord node : nodes) {
                        _connections.add(node.publicHostName, node.port);
                    }
                    // no nodes in the table: we may as well shut down and let ourselves respawn
                    if (nodes.size() == 0) {
                        log.info("No nodes found, shutting down");
                        shutdown();
                    }
                }
            });

        } catch (Exception e) {
            log.warning("Could not load nodes", e);
        }
    }
    
    protected void printSummary ()
    {
        int activeCount = 0;
        for (Bureau bureau : _bureaus.values()) {
            if (bureau.isRunning()) {
                ++activeCount;
            }
        }

        Object[] args = {"totalLaunched", _totalLaunched, "activeCount", activeCount, 
            "connectionCount", _connections._clients.size()};
        String msg = "Status";

        // Print summary to launcher log
        log.info(msg, args);

        // Print summary to merged log
        Logger.getLogger(BureauLogRedirector.class).info(msg, args);
    }
    
    protected void logRolled ()
    {
        log.info("Resetting bureau log limits");
        for (Iterator<Bureau> iter = _bureaus.values().iterator(); iter.hasNext(); ) {
            Bureau bureau = iter.next();
            if (bureau.isRunning()) {
                bureau.resetLogLimit();
            } else {
                iter.remove();
            }
        }
    }

    protected static class Bureau
    {
        public Bureau (String bureauId)
        {
            _bureauId = bureauId;
        }
        
        public void launch (String server, int port, String connectionToken)
        {
            // create the system command to execute
            String windowToken = StringUtil.md5hex(BureauLauncherConfig.windowSharedSecret);
            String [] command = {
                BureauLauncherConfig.serverRoot + "/bin/runthaneclient", "burl", _bureauId,
                connectionToken, server, String.valueOf(port), windowToken};
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);

            // attempt to launch the process
            try {
                _process = builder.start();
            } catch (java.io.IOException ioe) {
                log.warning("Could not launch thane", "bureauId", _bureauId, ioe);
                _message = "Unable to launch: " + ioe.getMessage();
                return;
            }
            
            _launchTime = System.currentTimeMillis();

            // truncate any running log and calculate the new limit
            int limit = BureauLauncherConfig.maximumLogSize;
            if (_redirector != null) {
                limit  = _redirector.getLimit() - _redirector.getWritten();
            }

            _redirector = new BureauLogRedirector(_bureauId, _process.getInputStream(), limit) {
                public void copyLoop () {
                    try {
                        super.copyLoop();
                    } finally {
                        _shutdownTime = System.currentTimeMillis();
                    }
                }
            };

            log.info("Launched thane", "command", command, "logLimit", limit);
            _message = "Successfully launched";
        }
        
        public boolean isRunning ()
        {
            return _redirector != null && _redirector.isRunning();
        }
        
        public void resetLogLimit ()
        {
            if (_redirector != null) {
                _redirector.reset(BureauLauncherConfig.maximumLogSize);
            }
        }
        
        public BureauLauncherInfo.BureauInfo getInfo ()
        {
            BureauLauncherInfo.BureauInfo info = new BureauLauncherInfo.BureauInfo();
            info.bureauId = _bureauId;
            info.launchTime = _launchTime;
            info.shutdownTime = _shutdownTime;
            info.message = _message;
            if (_redirector != null) {
                info.logSpaceRemaining = _redirector.getLimit() - _redirector.getWritten();
                if (info.logSpaceRemaining < 0) {
                    info.logSpaceRemaining = 0;
                }
                info.logSpaceUsed = _redirector.getWritten();
            }
            return info;
        }
        
        protected Process _process;
        protected String _bureauId;
        protected BureauLogRedirector _redirector;
        protected String _message;
        protected long _launchTime;
        protected long _shutdownTime;
    }
    
    /** Presents run queue. */
    @Inject protected Runner _runner;

    /** Database run queue. */
    protected Runner _dbrunner = new Runner();

    /** World and game server connections. */
    protected Connections _connections = new Connections(this);

    /** The last time a client logged off. */
    protected long _lastLogoffTime;

    /** Periodic job to poll the nodes table (posted on _dbrunner). */
    protected Interval _pollNodes = new Interval(_dbrunner) {
        public void expired () {
            pollForNewHosts();
        }
    };

    /** The total number of bureaus launched. */
    protected int _totalLaunched;

    /** The current set of bureaus on this launcher. Pruned periodically when printing a summary. */
    protected HashMap<String, Bureau> _bureaus = Maps.newHashMap();
    
    /** The nodes repository. */
    @Inject protected NodeRepository _nodeRepo;

    /** Time between checks of the <code>NODES</code> table. */
    protected static long PEER_POLL_INTERVAL = 30000;

    /** Time between checks of the <code>NODES</code> table when a recent logoff has been
     * encountered. */
    protected static long HYPER_PEER_POLL_INTERVAL = 1000;

    /** Duration to perform hyper polling before switching back to normal. */
    protected static long RESUME_NORMAL_INTERVAL = 10000;
}
