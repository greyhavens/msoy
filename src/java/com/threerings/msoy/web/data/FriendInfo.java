//
// $Id$

package com.threerings.msoy.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.web.data.MemberName;

/**
 * Contains information on a friend connection.
 */
public class FriendInfo
    implements IsSerializable
{
    /** Status constants. */
    public static final byte FRIEND = 0;
    public static final byte PENDING_MY_APPROVAL = 1;
    public static final byte PENDING_THEIR_APPROVAL = 2;

    /** The display name of the friend. */
    public MemberName name;

    /** Is the friend online? */
    public boolean online;

    /** The status of this friend (they might not be a friend yet). */
    public byte status;

    /** Used for deserialization. */
    public FriendInfo ()
    {
    }

    /** Creates a friend info with the supplied data. */
    public FriendInfo (MemberName name, boolean online, byte status)
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

    // @Override // from Object
    public int hashCode ()
    {
        return getMemberId();
    }

    // @Override // from Object
    public boolean equals (Object other)
    {
        return (other instanceof FriendInfo) &&
            (getMemberId() == ((FriendInfo)other).getMemberId());
    }

    // @Override // from Object
    public String toString ()
    {
        return "Friend[" + name + "]";
    }
}
