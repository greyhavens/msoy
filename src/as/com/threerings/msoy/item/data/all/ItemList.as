//
// $Id$

package com.threerings.msoy.item.data.all {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;
import com.threerings.io.TypedArray;

import com.threerings.presents.dobj.DSet_Entry;

/**
 * Represents a list of items created by a user.
 */
public class ItemList
    implements Streamable, DSet_Entry
{
    /** The summary info about this list. */
    public var info :ItemListInfo;

    /** The actual items in this ItemList. IF null, indicates that this is
     * a summary entry for the ItemList that does not contain the actual content.
     * If an ItemList is truly empty, then this will be a zero-element array.
     */
    public var items :TypedArray;

    public function ItemList ()
    {
    }

    // from DSet.Entry
    public function getKey () :Object
    {
        return info.getKey();
    }

    // from interface Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        info = ItemListInfo(ins.readObject());
        items = TypedArray(ins.readObject());
    }

    // from interface Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeObject(info);
        out.writeObject(items);
    }
}
}
