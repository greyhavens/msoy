//
// $Id$

package com.threerings.msoy.data.all {

import com.threerings.io.ObjectOutputStream;
import com.threerings.io.ObjectInputStream;
import com.threerings.presents.dobj.DSet_Entry;

/**
 * Represents a friend connection.
 */
public class FriendEntry extends PlayerEntry
{
    /** This player's current status. */
    public var status :String;

    /** Is the friend online? */
    public var online :Boolean;

    /** Mr. Constructor. */
    public function FriendEntry (
        name :VizMemberName = null, status :String = null, online :Boolean = false)
    {
        super(name);
        this.status = status;
        this.online = online;
    }

    override public function toString () :String
    {
        return "FriendEntry[" + name + "]";
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        status = (ins.readField(String) as String);
        online = ins.readBoolean();
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        out.writeField(status);
        out.writeBoolean(online);
    }
}
}
