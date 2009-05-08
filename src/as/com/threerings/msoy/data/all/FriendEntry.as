//
// $Id$

package com.threerings.msoy.data.all {

import com.threerings.io.ObjectOutputStream;
import com.threerings.io.ObjectInputStream;

/**
 * Represents a friend connection.
 */
public class FriendEntry extends PlayerEntry
{
    /** This player's current status. */
    public var status :String;

    override public function toString () :String
    {
        return "FriendEntry[" + name + "]";
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        status = (ins.readField(String) as String);
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeField(status);
    }
}
}
