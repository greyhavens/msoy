//
// $Id$

package com.threerings.msoy.data.all;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.presents.dobj.DSet;

/**
 * Represents a friend connection.
 */
public class FriendEntry extends PlayerEntry
{
    /** This player's current status. */
    public String status;

    /** Is the friend online? */
    public boolean online;

    /** Suitable for deserialization. */
    public FriendEntry ()
    {
    }

    /** Mr. Constructor. */
    public FriendEntry (VizMemberName name, String status, boolean online)
    {
    	super(name);
        this.status = status;
        this.online = online;
    }

    @Override
    public String toString ()
    {
        return "FriendEntry[" + name + "]";
    }
}
