//
// $Id$

package com.threerings.msoy.world.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.SimpleStreamableObject;
import com.threerings.presents.dobj.DSet_Entry;

import com.threerings.msoy.item.data.all.ItemIdent;

/**
 * Used to coordinate the "control" of a particular scene entity. The client that is in control of
 * the entity is the only one that will be allowed to make changes to the entity's distributed
 * state.
 */
public class EntityControl extends SimpleStreamableObject
    implements DSet_Entry
{
    /** Identifies the item being controlled. */
    public var ident :ItemIdent;

    /** The body oid of the client currently controlling this entity. */
    public var controllerOid :int;

    public function EntityControl ()
    {
    }

    // from interface DSet_Entry
    public function getKey () :Object
    {
        return ident;
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        ident = (ins.readObject() as ItemIdent);
        controllerOid = ins.readInt();
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeObject(ident);
        out.writeInt(controllerOid);
    }
}
}
