//
// $Id$

package com.threerings.msoy.game.client;

import com.samskivert.util.Logger;
import com.samskivert.util.RunQueue;
import com.samskivert.util.StringUtil;
import com.threerings.msoy.game.data.MsoyBureauLauncherCredentials;
import com.threerings.msoy.game.data.MsoyBureauLauncherCodes;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.presents.client.BlockingCommunicator;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientAdapter;
import com.threerings.presents.client.Communicator;
import com.threerings.presents.net.Credentials;

import static com.threerings.msoy.Log.log;

/**
 * Connects to the msoy game server and receives requests to launch bureaus.
 */
public class MsoyBureauLauncherClient extends Client
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
        MsoyBureauLauncherClient client;

        String[] serverList = ServerConfig.bureauGameServers.split(",");
        for (int ii = 0; ii < serverList.length; ii++) {
            serverList[ii] = serverList[ii].trim();
            log.info("Connecting to " + serverList[ii]);
            Runner queue = new Runner();
            client = new MsoyBureauLauncherClient(queue);
            client.setServer(serverList[ii]);
            client.logon();
            queue.run();
            if (client.wasLoggedOn()) {
                log.info("Logon successful, exiting");
                break;
            }
            log.info("Logon failed");
        }
    }

    /**
     * Creates a new bureau launcher client, setting up bureau launcher authentation.
     * @see MsoyBureauLauncherCredentials
     * @see MsoyBureauLauncherAuthenticator
     * @see ServerConfig#bureauSharedSecret
     */
    public MsoyBureauLauncherClient (Runner queue)
    {
        super(new MsoyBureauLauncherCredentials(
            ServerConfig.serverHost, 
            ServerConfig.bureauSharedSecret), queue);

        log.info("Created credentials: " + _creds);
        addClientObserver(new Observer());
        addServiceGroup(MsoyBureauLauncherCodes.BUREAU_LAUNCHER_GROUP);
    }

    /**
     * Returns true if the client ever succeeded in logging on.
     */
    public boolean wasLoggedOn ()
    {
        return _loggedOn;
    }

    /**
     * Set the server to connect to to the given server:port.
     */
    public void setServer (String serverNameAndPort)
    {
        int colon = serverNameAndPort.indexOf(':');
        if (colon == -1) {
            throw new Error("invalid config, no port number on " + serverNameAndPort);
        }
        String server = serverNameAndPort.substring(0, colon);
        String port = serverNameAndPort.substring(colon + 1);
        setServer(server, new int [] {Integer.parseInt(port)});
    }

    @Override
    protected Communicator createCommunicator ()
    {
        return new BlockingCommunicator(this);
    }

    // Observe the connection progress, stopping the run queue
    // on failure or logoff
    protected class Observer extends ClientAdapter
    {
        public void clientDidLogon (Client client)
        {
            _loggedOn = true;
            _service = getService(MsoyBureauLauncherService.class);
            _service.launcherInitialized(MsoyBureauLauncherClient.this);
        }

        public void clientFailedToLogon (Client client, Exception cause)
        {
            log.warning("failed to logon", cause);
            stop();
        }

        public void clientDidLogoff (Client client)
        {
            stop();
        }

        public void stop ()
        {
            ((Runner)_runQueue).stop();
        }
    }

    protected boolean _loggedOn;
    protected MsoyBureauLauncherService _service;
}
