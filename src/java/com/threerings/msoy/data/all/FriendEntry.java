//
// $Id$

package com.threerings.msoy.data.all;

/**
 * Represents a friend connection.
 */
public class FriendEntry extends PlayerEntry
{
    /** This player's current status. */
    public String status;

    /** Suitable for deserialization. */
    public FriendEntry ()
    {
    }

    /** Mr. Constructor. */
    public FriendEntry (VizMemberName name, String status)
    {
    	super(name);
        this.status = status;
    }

    @Override
    public String toString ()
    {
        return "FriendEntry[" + name + "]";
    }
}
