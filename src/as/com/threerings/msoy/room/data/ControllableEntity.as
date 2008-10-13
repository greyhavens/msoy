//
// $Id$

package com.threerings.msoy.room.data {

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

    override public function equals (other :Object) :Boolean
    {
        return ((other is ControllableEntity) && _ident != null &&
                _ident.equals((other as ControllableEntity).getItemIdent()));
    }

    public function getItemIdent () :ItemIdent
    {
        return _ident;
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        _ident = ItemIdent(ins.readObject());
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
