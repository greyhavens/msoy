//
// $Id$

package com.threerings.msoy.data;

import com.threerings.util.Name;

import com.threerings.presents.dobj.DSet;

/**
 * Represents a friend connection.
 */
public class FriendEntry
    implements Comparable, DSet.Entry
{
    /** The immutable memberId for the friend. */
    public int memberId;

    /** The display name of the friend. */
    public Name name;

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
    public FriendEntry (int memberId, Name name, boolean online, byte status)
    {
        this.memberId = memberId;
        this.name = name;
        this.online = online;
        this.status = status;
    }

    // from interface DSet.Entry
    public Comparable getKey ()
    {
        return memberId;
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
