package com.threerings.msoy.bureau.client;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.util.Interval;
import com.samskivert.util.ProcessLogger;
import com.samskivert.util.RunQueue;
import com.threerings.msoy.bureau.server.BureauLauncherConfig;
import com.threerings.presents.server.SunSignalHandler;
import com.threerings.presents.server.ShutdownManager;
import com.threerings.presents.server.NativeSignalHandler;
import com.threerings.presents.annotation.EventQueue;
import com.threerings.presents.peer.server.persist.NodeRecord;
import com.threerings.presents.peer.server.persist.NodeRepository;

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

            // TODO: what is "userdb" and should we change it?
            bind(PersistenceContext.class).toInstance(new PersistenceContext("userdb", provider));
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

        protected Thread _dispatcher = Thread.currentThread();
        protected java.util.List<Runnable> _queue =
            java.util.Collections.synchronizedList(
                new java.util.LinkedList<Runnable>());
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

        new Thread() {
            public void run () {
                _dbrunner.run();
            }
        }.start();

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
        // TODO: should this go on an invoker thread? Normally, yes, but this is only going to be
        // called when the first instance of a game is played since the last server restart, so it
        // is debatable.
        String [] command = {
            BureauLauncherConfig.serverRoot + "/bin/runthaneclient",
            "burl", bureauId, token, server, String.valueOf(port)};
        log.info("Attempting to launch thane", "command", command);
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        try {
            Process process = builder.start();
            // log the output of the process and prefix with bureau id
            ProcessLogger.copyMergedOutput(log, bureauId, process);

        } catch (java.io.IOException ioe) {
            log.warning("Could not launch thane", "bureauId", bureauId, ioe);
        }
    }

    // from BureauLauncherReceiver
    public void shutdownLauncher ()
    {
        shutdown();
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

        } catch (PersistenceException pe) {
            log.warning("Could not load nodes", pe);
        }
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

    /** The nodes repository. */
    @Inject protected NodeRepository _nodeRepo;

    /** Time between checks of the <code>NODES</code> table. */
    protected static long PEER_POLL_INTERVAL = 60000;

    /** Time between checks of the <code>NODES</code> table when a recent logoff has been
     * encountered. */
    protected static long HYPER_PEER_POLL_INTERVAL = 60000;

    /** Time between checks of the <code>NODES</code> table. */
    protected static long RESUME_NORMAL_INTERVAL = 10000;

}
