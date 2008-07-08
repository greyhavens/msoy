package com.threerings.msoy.bureau.client;

import com.samskivert.util.ProcessLogger;
import com.samskivert.util.RunQueue;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientAdapter;

import static com.threerings.msoy.Log.log;

public class BureauLauncher
    implements BureauLauncherReceiver
{
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
                    log.warning("Runnable " + r + " threw", t);
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
        BureauLauncher launcher = new BureauLauncher();
        launcher.run();
    }

    /**
     * Creates a new bureau launcher.
     */
    public BureauLauncher ()
    {
        _runner = new Runner();
    }

    /**
     * Runs the bueau launcher.
     */
    public void run ()
    {
        BureauLauncherClient client;

        String[] serverList = ServerConfig.bureauGameServers.split(",");
        for (int ii = 0; ii < serverList.length; ii++) {
            serverList[ii] = serverList[ii].trim();
            log.info("Connecting to " + serverList[ii]);
            _runner = new Runner();
            client = new BureauLauncherClient(this);
            client.setServer(serverList[ii]);
            client.addClientObserver(new Observer());
            _loggedOn = false;
            client.logon();
            _runner.run();
            if (_loggedOn) {
                log.info("Logon was successful");
                break;
            }
            log.info("Logon failed");
        }
        log.info("Exiting");
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

    // Observe the connection progress, stopping the run queue
    // on failure or logoff
    protected class Observer extends ClientAdapter
    {
        public void clientDidLogon (Client client)
        {
            _loggedOn = true;
        }

        public void clientFailedToLogon (Client client, Exception cause)
        {
            log.warning("failed to logon", cause);
            _runner.stop();
        }

        public void clientDidLogoff (Client client)
        {
            _runner.stop();
        }
    }

    protected Runner _runner;
    protected boolean _loggedOn;
}
