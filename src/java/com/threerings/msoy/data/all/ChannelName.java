//
// $Id$

package com.threerings.msoy.data.all;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.util.Name;

/**
 * Represents the name of a custom chat channel.
 */
public class ChannelName extends Name
    implements IsSerializable
{
    /** The maximum length of a channel name */
    public static final int LENGTH_MAX = 24;

    /** The minimum length of a channel name */
    public static final int LENGTH_MIN = 3;

    /** Used when unserializing */
    public ChannelName ()
    {
    }

    /**
     * Creates an instance with the specified name and creator.
     */
    public ChannelName (String name, int creatorId)
    {
        super(name);
        _creatorId = creatorId;
    }

    /**
     * Returns the member id of this channel's creator.
     */
    public int getCreatorId ()
    {
        return _creatorId;
    }

    // @Override // from Object
    public int hashCode ()
    {
        return _name.hashCode() ^ _creatorId;
    }

    // @Override // from Object
    public boolean equals (Object other)
    {
        if (other instanceof ChannelName) {
            ChannelName oc = (ChannelName)other;
            return _creatorId == oc._creatorId && _name.equals(oc._name);
        }
        return  false;
    }

    // @Override // from Name
    public int compareTo (Name o) 
    {
        ChannelName oc = (ChannelName)o;
        if (_creatorId == oc._creatorId) {
            return super.compareTo(oc);
        } else {
            return _creatorId - oc._creatorId;
        }
    }

    /** The member id of this channel's creator. */
    protected int _creatorId;
}
