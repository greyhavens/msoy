//
// $Id$

package com.threerings.msoy.data;

import com.threerings.presents.dobj.DSet;

import com.threerings.msoy.web.data.FriendInfo;
import com.threerings.msoy.web.data.MemberName;

/**
 * Represents a friend connection.
 */
public class FriendEntry extends FriendInfo
    implements Comparable, DSet.Entry
{
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

    // from interface DSet.Entry
    public Comparable getKey ()
    {
        return getMemberId();
    }

    // from interface Comparable
    public int compareTo (Object other)
    {
        FriendEntry that = (FriendEntry) other;
        // real friends go above not-yet-friends (of whatever kind)
        if ((this.status == FRIEND) != (that.status == FRIEND)) {
            return (this.status == FRIEND) ? -1 : 1;
        }
        // online folks show up above offline folks
        if (this.online != that.online) {
            return this.online ? -1 : 1;
        }
        // then, sort by name
        return this.name.compareTo(that.name);
    }
}
