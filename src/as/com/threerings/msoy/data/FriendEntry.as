package com.threerings.msoy.data {

import com.threerings.util.Comparable;

import com.threerings.io.ObjectOutputStream;
import com.threerings.io.ObjectInputStream;

import com.threerings.presents.dobj.DSet_Entry;

public class FriendEntry
    implements Comparable, DSet_Entry
{
    /** The display name of this friend. */
    public var name :MemberName;

    /** Is the friend online? */
    public var online :Boolean;

    /** The status of this friend. */
    public var status :int;

    /** Status constants. */
    public static const FRIEND :int = 0;
    public static const PENDING_MY_APPROVAL :int = 1;
    public static const PENDING_THEIR_APPROVAL :int = 2;

    /** Mr. Constructor. */
    public function FriendEntry (
            name :MemberName = null, online :Boolean = false, status :int = 0)
    {
        this.name = name;
        this.online = online;
        this.status = status;
    }

    public function getMemberId () :int
    {
        return name.getMemberId();
    }

    // from interface DSet_Entry
    public function getKey () :Object
    {
        return getMemberId();
    }

    // from interface Comparable
    public function compareTo (other :Object) :int
    {
        var that :FriendEntry = (other as FriendEntry);
        // real friends go above not-yet-friends
        var c :int = this.status - that.status;
        if (c != 0) {
            return c;
        }
        // online folks show up above offline folks
        if (this.online != that.online) {
            return this.online ? -1 : 1;
        }
        // then, sort by name
        return MemberName.BY_DISPLAY_NAME(this.name, that.name);
    }

    // from interface Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeObject(name);
        out.writeBoolean(online);
        out.writeByte(status);
    }

    // from interface Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        name = (ins.readObject() as MemberName);
        online = ins.readBoolean();
        status = ins.readByte();
    }
}
}
