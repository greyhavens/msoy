//
// $Id$

package com.threerings.msoy.data;

import com.threerings.util.Name;

import com.threerings.presents.dobj.DSet;

/**
 * Does something extraordinary.
 */
public class FriendEntry
    implements Comparable, DSet.Entry
{
    /** The name of the friend. */
    public Name name;

    /** Is the friend online? */
    public boolean online;

    /** Suitable for deserialization. */
    public FriendEntry ()
    {
    }

    /** Mr. Constructor. */
    public FriendEntry (Name name, boolean online)
    {
        this.name = name;
        this.online = online;
    }

    // from interface DSet.Entry
    public Comparable getKey ()
    {
        return name;
    }

    // from interface Comparable
    public int compareTo (Object other)
    {
        FriendEntry that = (FriendEntry) other;
        // online folks show up above offline folks
        if (this.online != that.online) {
            return this.online ? -1 : 1;
        }
        // then, sort by name
        return this.name.compareTo(that.name);
    }
}
