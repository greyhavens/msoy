//
// $Id$

package com.threerings.msoy.bureau.client;

import com.google.common.collect.Maps;
import com.samskivert.util.Interval;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientAdapter;

import static com.threerings.msoy.Log.log;

/**
 * Manages a collection of bureau launcher clients (private to package).
 */
class Connections
{
    /**
     * Time that a server will be blacklisted after a logon failure.
     */
    public static long CLIENT_RENEW_TIME = 15 * 1000;

    /**
     * Time between checks to renew (unblacklist) servers.
     */
    protected static long CLIENT_RENEW_INTERVAL = 15 * 1000;

    /**
     * Creates a new set of clients that will be created using the given launcher.
     * The run queue of the launcher will also be used to perform maintenance jobs
     * on the collection.
     */
    public Connections (BureauLauncher launcher)
    {
        _launcher = launcher;

        _purge = new Interval(_launcher.getRunner()) {
            public void expired () {
                purgeFailedConnections();
            }
        };

        _purge.schedule(CLIENT_RENEW_INTERVAL, true);
    }

    /**
     * Add a new connection to the given server. Logon is automatically attempted. If logon
     * fails, the server is blacklisted for a given amount of time such that calls to
     * <code>add</code> the server again will be ignored.
     * @see #CLIENT_RENEW_TIME
     */
    public void add (String host, int port)
    {
        if (_shutdown) {
            return;
        }

        String key = host + ":" + port;
        Entry entry = _clients.get(key);
        if (entry != null) {
            return;
        }

        log.info("Connecting", "host", host, "port", port);
        entry = new Entry(host, port);
        _clients.put(key, entry);
    }

    public void shutdown ()
    {
        if (_shutdown) {
            return;
        }

        _shutdown = true;
        _purge.cancel();
        java.util.List<Entry> entries = new java.util.ArrayList<Entry>();
        entries.addAll(_clients.values());
        for (Entry entry : entries) {
            if (entry.getState() == State.CONNECTED ||
                entry.getState() == State.PENDING) {
                if (entry._client.isActive()) {
                    entry.logoff();
                }
            }
        }
    }

    /**
     * Remove blacklisted connections that have waited long enough.
     */
    protected void purgeFailedConnections ()
    {
        long now = System.currentTimeMillis();
        java.util.List<String> keys = new java.util.ArrayList<String>();
        keys.addAll(_clients.keySet());
        for (String key : keys) {
            Entry entry = _clients.get(key);
            if (entry.getState() != State.FAILED) {
                continue;
            }
            long age = now - entry.getLastUpdateTime();
            if (age >= CLIENT_RENEW_TIME) {
                log.info("Clearing previously failed connection", "entry", entry);
                _clients.remove(key);
            }
        }
    }

    /**
     * Observe the connection progress, transitioning our internal connection state.
     */
    protected class Observer extends ClientAdapter
    {
        @Override // from ClientAdapter
        public void clientDidLogon (Client client)
        {
            Entry entry = getEntry(client, "logon", State.PENDING);
            if (entry != null) {
                log.info("Client connected", "entry", entry);
                entry.setState(State.CONNECTED);
            }
        }

        @Override // from ClientAdapter
        public void clientFailedToLogon (Client client, Exception cause)
        {
            Entry entry = getEntry(client, "failure to logon", State.PENDING);
            if (entry != null) {
                log.info("Client failed to logon", "entry", entry, cause);
                entry.setState(State.FAILED);
            }
        }

        @Override // from ClientAdapter
        public void clientDidLogoff (Client client)
        {
            Entry entry = getEntry(client, "logoff", State.CONNECTED);
            if (entry != null) {
                log.info("Client logged off, removing", "entry", entry);
                Entry verify = _clients.remove(key(client));
                if (verify == null) {
                    log.info("Client not in map", "client", client);
                }
            }
            _launcher.clientLoggedOff();
        }

        /**
         * Retrieves an entry that should be in a certain state. Uses the caller in warning
         * messages.
         */
        protected Entry getEntry (Client client, String caller, State expected)
        {
            Entry entry = _clients.get(key(client));
            if (entry == null) {
                log.warning(caller + " observed for unknown client", "client", client);
                return null;

            } else if (entry.getState() != expected) {
                log.warning(caller + " observed for client in unexpected state", "entry", entry);
                return null;
            }

            return entry;
        }
    }

    /**
     * The client states that we care about.
     */
    protected static enum State
    {
        PENDING,
        CONNECTED,
        FAILED
    }

    /**
     * Joins a client to a state value and the last time the state changed.
     */
    protected class Entry
    {
        /**
         * Creates a new entry with a client in the pending state attempting logon to the given
         * server.
         */
        public Entry (String host, int port)
        {
            _client = new BureauLauncherClient(_launcher, Connections.this);
            _client.setServer(host, new int[]{port});
            _client.addClientObserver(_observer);
            setState(State.PENDING);

            _client.logon();
        }

        // from Object
        public String toString ()
        {
            StringBuffer buff = new StringBuffer();
            buff.append("[updateTime: ").append(_updateTime);
            buff.append(", state: ").append(_state);
            buff.append(", client: ").append(_client);
            buff.append("]");
            return buff.toString();
        }

        /**
         * Accesses the current state. Never null.
         */
        public State getState ()
        {
            return _state;
        }

        /**
         * Sets the current state to the given value and sets the last update time to the
         * current time.
         */
        public void setState (State state)
        {
            _state = state;
            _updateTime = System.currentTimeMillis();
        }

        /**
         * Accesses the time of the last state change.
         */
        public long getLastUpdateTime ()
        {
            return _updateTime;
        }

        /**
         * Logs off the client.
         */
        public void logoff ()
        {
            _client.logoff(false);
        }

        protected State _state;
        protected long _updateTime;
        protected BureauLauncherClient _client;
    }

    protected static String key (Client client)
    {
        return client.getHostname() + ":" + client.getPorts()[0];
    }

    protected BureauLauncher _launcher;
    protected Observer _observer =  new Observer();
    protected java.util.Map<String, Entry> _clients = Maps.newHashMap();
    protected Interval _purge;
    protected boolean _shutdown;
}
