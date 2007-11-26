//
// $Id$

package com.threerings.msoy.data.all {

import com.threerings.util.Comparable;
import com.threerings.util.Hashable;

import com.threerings.io.ObjectOutputStream;
import com.threerings.io.ObjectInputStream;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.item.data.all.MediaDesc;

import com.threerings.presents.dobj.DSet_Entry;

/**
 * Represents a friend connection.
 */
public class FriendEntry
    implements Comparable, DSet_Entry, Hashable
{
    /** The display name of the friend. */
    public var name :MemberName;

    /** Is the friend online? */
    public var online :Boolean;

    /** This friend's current profile photo. */
    public var photo :MediaDesc;

    /** Mr. Constructor. */
    public function FriendEntry (
        name :MemberName = null, online :Boolean = false, photo: MediaDesc = null)
    {
        this.name = name;
        this.online = online;
        this.photo = photo;
    }

    /**
     * Get the member id of this friend.
     */
    public function getMemberId () :int
    {
        return name.getMemberId();
    }

    // from Hashable
    public function hashCode () :int
    {
        return getMemberId();
    }

    // from interface Comparable
    public function compareTo (other :Object) :int
    {
        var that :FriendEntry = (other as FriendEntry);
        // online folks show up above offline folks
        if (this.online != that.online) {
            return this.online ? -1 : 1;
        }
        // then, sort by name
        return MemberName.BY_DISPLAY_NAME(this.name, that.name);
    }

    // from Hashable
    public function equals (other :Object) :Boolean
    {
        return (other is FriendEntry) &&
            (getMemberId() == (other as FriendEntry).getMemberId());
    }

    //
    public function toString () :String
    {
        return "FriendEntry[" + name + "]";
    }

    // from interface DSet_Entry
    public function getKey () :Object
    {
        return getMemberId();
    }

    // from interface Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        name = (ins.readObject() as MemberName);
        online = ins.readBoolean();
        photo = (ins.readObject() as MediaDesc);
    }

    // from interface Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeObject(name);
        out.writeBoolean(online);
        out.writeObject(photo);
    }
}
}
