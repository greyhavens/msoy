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
import com.samskivert.util.StringUtil;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.presents.client.Client;
import com.threerings.presents.peer.server.persist.NodeRecord;
import com.threerings.presents.peer.server.persist.NodeRepository;

import static com.threerings.msoy.Log.log;

public class BureauLauncher
    implements BureauLauncherReceiver
{
    /** Guice module for bureau launcher. */
    public static class Module extends AbstractModule
    {
        @Override protected void configure () {
            ConnectionProvider provider = null;
            try {
                provider = ServerConfig.createConnectionProvider();
            } catch (Exception e) {
                addError(e);
            }

            // TODO: what is "userdb" and should we change it?
            bind(PersistenceContext.class).toInstance(
                new PersistenceContext("userdb", provider));
        }
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
                if (_queue.isEmpty()) {
                    if (_stopped) {
                        break;
                    }
                    try {
                        synchronized (this) {
                            wait();
                        }
                    } catch (InterruptedException ie) {
                        // what to do?
                    }
                }

                Runnable r = _queue.remove(0);
                try {
                    r.run();
                } catch (Throwable t) {
                    log.warning("Error executing run queue item", "runnable", r, t);
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
     * Runs the client program. The arguments are not used and all data is read from 
     * server.properties. The servers declared in {@link ServerConfig#bureauGameServers} are 
     * connected to in turn until one of them succeeds. The successful connection is kept open 
     * indefinitely, after which the method exits.
     */
    public static void main (String[] args)
    {
        Injector injector = Guice.createInjector(new Module());
        BureauLauncher launcher = injector.getInstance(BureauLauncher.class);
        launcher.run();
    }

    /**
     * Creates a new bureau launcher.
     */
    @Inject public BureauLauncher ()
    {
    }

    /**
     * Runs the bureau launcher.
     */
    public void run ()
    {
        new Interval(_dbrunner) {
            public void expired () {
                pollForNewHosts();
            }
        }.schedule(0, PEER_POLL_INTERVAL);

        new Thread() {
            public void run () {
                _dbrunner.run();
            }
        }.start();

        _runner.run();

        log.info("Exiting");
    }

    public void pollForNewHosts ()
    {
        try {
            final java.util.Collection<NodeRecord> nodes = _nodeRepo.loadNodes();

            _runner.postRunnable(new Runnable() {
                public void run() {
                    for (NodeRecord node : nodes) {
                        _connections.add(node.publicHostName, node.port);
                    }
                }
            });
            
        } catch (PersistenceException pe) {
            log.warning("Could not load nodes", pe);
        }
    }

    /**
     * Accesses the run queue held by the launcher.
     */
    public RunQueue getRunner ()
    {
        return _runner;
    }

    /**
     * Launches a thane bureau client.
     */
    public void launchThane (
        String bureauId,
        String token,
        String server,
        int port)
    {
        // TODO: should this go on an invoker thread? Normally, yes, but this is only going to be 
        // called when the first instance of a game is played since the last server restart, so it
        // is debatable.
        String [] command = {
            ServerConfig.serverRoot + "/bin/runthaneclient",
            bureauId, token, server, String.valueOf(port)};
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

    protected Runner _runner = new Runner();
    protected Runner _dbrunner = new Runner();
    @Inject protected NodeRepository _nodeRepo;
    protected Connections _connections = new Connections(this);
    protected static long PEER_POLL_INTERVAL = 60000;
}
