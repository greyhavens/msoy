//
// $Id$

package com.threerings.msoy.item.data.all {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;
import com.threerings.io.TypedArray;

import com.threerings.presents.dobj.DSet_Entry;

public class ItemList
    implements Streamable, DSet_Entry
{
    public var info :ItemListInfo;

    public var items :TypedArray;

    public function ItemList ()
    {
    }

    public function getKey () :Object
    {
        return info.getKey();
    }

    // from interface Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        info = ins.readObject() as ItemListInfo;
        items = (ins.readObject() as TypedArray);
    }

    // from interface Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeObject(info);
        out.writeObject(items);
    }
}
}
