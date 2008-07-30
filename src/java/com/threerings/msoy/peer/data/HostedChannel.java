//
// $Id$

package com.threerings.msoy.peer.data;

import com.threerings.io.SimpleStreamableObject;
import com.threerings.msoy.chat.data.ChatChannel;
import com.threerings.presents.dobj.DSet;

/**
 * Represents a chat channel hosted on one of the peers.
 * Just a DSet wrapper around a ChatChannel.
 */
public class HostedChannel extends SimpleStreamableObject
    implements DSet.Entry
{
    /** Channel definition, includes type and name info. */
    public ChatChannel channel;

    /** Distributed object id of this channel on the hosting peer. */
    public int oid;

    // from DSet.Entry
    public Comparable<?> getKey ()
    {
        return getKey(channel);
    }

    /** Static version of the channel key generator. */
    public static Comparable<?> getKey (ChatChannel channel)
    {
        return channel.hashCode();
    }

    /** Used when unserializing. */
    public HostedChannel ()
    {
    }

    /**
     * Creates a hosted channel record.
     */
    public HostedChannel (ChatChannel channel, int oid)
    {
        this.channel = channel;
        this.oid = oid;
    }
}
