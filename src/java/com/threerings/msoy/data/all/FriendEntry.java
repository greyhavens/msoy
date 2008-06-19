//
// $Id$

package com.threerings.msoy.data.all;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.presents.dobj.DSet;

import com.threerings.msoy.item.data.all.MediaDesc;

/**
 * Represents a friend connection.
 */
public class FriendEntry
    implements Comparable, DSet.Entry, IsSerializable
{
    /** The display name of the friend. */
    public MemberName name;

    /** Is the friend online? */
    public boolean online;

    /** This friend's current profile photo. */
    public MediaDesc photo;

    /** Suitable for deserialization. */
    public FriendEntry ()
    {
    }

    /** Mr. Constructor. */
    public FriendEntry (MemberName name, boolean online, MediaDesc photo)
    {
        this.name = name;
        this.online = online;
        this.photo = photo;
    }

    /**
     * Get the member id of this friend.
     */
    public int getMemberId ()
    {
        return name.getMemberId();
    }

    // from interface DSet.Entry
    public Comparable getKey ()
    {
        return new Integer(getMemberId());
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

    @Override // from Object
    public int hashCode ()
    {
        return getMemberId();
    }

    @Override // from Object
    public boolean equals (Object other)
    {
        return (other instanceof FriendEntry) &&
            (getMemberId() == ((FriendEntry)other).getMemberId());
    }

    @Override
    public String toString ()
    {
        return "FriendEntry[" + name + "]";
    }
}
