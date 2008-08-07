//
// $Id$

package com.threerings.msoy.room.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.SimpleStreamableObject;
import com.threerings.presents.dobj.DSet_Entry;

/**
 * Used to coordinate the "control" of some executable client content - currently either a room
 * entity (item) or an AVRG. This mechanism elevates one specific client-side instance to play the
 * role usually reserved for server-side logic.
 */
public class EntityControl extends SimpleStreamableObject
    implements DSet_Entry
{
    /** Identifies what is being controlled. */
    public var controlled :Controllable;

    /** The body oid of the client in control of this controllable. */
    public var controllerOid :int;

    public function EntityControl ()
    {
    }

    // from interface DSet_Entry
    public function getKey () :Object
    {
        return controlled;
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        controlled = (ins.readObject() as Controllable);
        controllerOid = ins.readInt();
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeObject(controlled);
        out.writeInt(controllerOid);
    }
}
}
