//
// $Id$

package com.threerings.msoy.data;

import com.threerings.presents.dobj.DSet;

import com.threerings.msoy.web.data.FriendInfo;

/**
 * Represents a friend connection.
 */
public class FriendEntry
    implements Comparable, DSet.Entry
{
    /** The display name of the friend. */
    public MemberName name;

    /** Is the friend online? */
    public boolean online;

    /** The status of this friend (they might not be a friend yet). */
    public byte status;

    /** Status constants. */
    public static final byte FRIEND = 0;
    public static final byte PENDING_MY_APPROVAL = 1;
    public static final byte PENDING_THEIR_APPROVAL = 2;

    /** Suitable for deserialization. */
    public FriendEntry ()
    {
    }

    /** Mr. Constructor. */
    public FriendEntry (MemberName name, boolean online, byte status)
    {
        this.name = name;
        this.online = online;
        this.status = status;
    }

    /**
     * Get the member id of this friend.
     */
    public int getMemberId ()
    {
        return name.getMemberId();
    }

    /**
     * Converts this record into a simpler format that we can send to the web
     * client.
     */
    public FriendInfo toInfo ()
    {
        FriendInfo info = new FriendInfo();
        info.memberId = name.getMemberId();
        info.name = name.toString();
        info.status = status;
        return info;
    }

    // from interface DSet.Entry
    public Comparable getKey ()
    {
        return getMemberId();
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
