//
// $Id$

package com.threerings.msoy.data.all {

import com.threerings.io.ObjectOutputStream;
import com.threerings.io.ObjectInputStream;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.MediaDesc;

/**
 * Represents a friend connection.
 */
public class FriendEntry
    implements PeerEntry
{
    /** The display name of the friend. */
    public var name :MemberName;

    /** Is the friend online? */
    public var online :Boolean;

    /** This friend's current profile photo. */
    public var photo :MediaDesc;

    /** This friend's current status. */
    public var status :String;

    /** Mr. Constructor. */
    public function FriendEntry (
        name :MemberName = null, online :Boolean = false, photo: MediaDesc = null, 
        status :String = null)
    {
        this.name = name;
        this.online = online;
        this.photo = photo;
        this.status = status;
    }

    /**
     * Get the member id of this friend.
     */
    public function getMemberId () :int
    {
        return name.getMemberId();
    }

    public function getName () :MemberName
    {
        return name;
    }

    public function getPhoto () :MediaDesc
    {
        return photo;
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
        name = MemberName(ins.readObject());
        online = ins.readBoolean();
        photo = MediaDesc(ins.readObject());
        status = (ins.readField(String) as String);
    }

    // from interface Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeObject(name);
        out.writeBoolean(online);
        out.writeObject(photo);
        out.writeField(status);
    }
}
}
