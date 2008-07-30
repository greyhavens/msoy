package com.threerings.msoy.bureau.data;

import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.DSet;

/**
 * Distributed registry of server information.
 */
public class ServerRegistryObject extends DObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>servers</code> field. */
    public static final String SERVERS = "servers";
    // AUTO-GENERATED: FIELDS END

    /**
     * Information about a single server for use when connecting.
     */
    public static class ServerInfo
        implements DSet.Entry
    {
        public String hostName;
        public int port;

        public ServerInfo (String hostName, int port)
        {
            this.hostName = hostName;
            this.port = port;
        }

        public ServerInfo ()
        {
        }

        // from DSet.Entry
        public Comparable<?> getKey ()
        {
            return hostName + ":" + port;
        }
    }

    /** Set of all servers that are being managed by the owner of this registry. */
    public DSet<ServerInfo> servers = new DSet<ServerInfo>();

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the specified entry be added to the
     * <code>servers</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToServers (ServerRegistryObject.ServerInfo elem)
    {
        requestEntryAdd(SERVERS, servers, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>servers</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromServers (Comparable<?> key)
    {
        requestEntryRemove(SERVERS, servers, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>servers</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateServers (ServerRegistryObject.ServerInfo elem)
    {
        requestEntryUpdate(SERVERS, servers, elem);
    }

    /**
     * Requests that the <code>servers</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setServers (DSet<ServerRegistryObject.ServerInfo> value)
    {
        requestAttributeChange(SERVERS, value, this.servers);
        DSet<ServerRegistryObject.ServerInfo> clone = (value == null) ? null : value.typedClone();
        this.servers = clone;
    }
    // AUTO-GENERATED: METHODS END
}
