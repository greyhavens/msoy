//
// $Id$

package com.threerings.msoy.world.data {

import com.threerings.io.ObjectOutputStream;
import com.threerings.io.ObjectInputStream;

import com.threerings.msoy.item.data.all.ItemIdent;

/**
 * A reference to a controllable entity.
 */
public class ControllableEntity extends Controllable
{
    public function ControllableEntity (ident :ItemIdent = null)
    {
        _ident = ident;
    }

    override public function getKey () :Object
    {
        return _ident;
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        _ident = (ins.readObject() as ItemIdent);
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeObject(_ident);
    }

    protected var _ident :ItemIdent;
}
}
