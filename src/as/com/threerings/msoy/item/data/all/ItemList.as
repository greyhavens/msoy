//
// $Id$

package com.threerings.msoy.item.data.all {
public class ItemList
    implements Streamable, DSet_Entry
{
    public var listId :int;

    public var ownerId :int;

    public var name :String;

    public var items :TypedArray;

    public function ItemList ()
    {
        TODO: IMPLEMENT ME
    }

    public function getKey () :Comparable
    {
        TODO: IMPLEMENT ME
    }

    // from interface Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        listId = ins.readInt();
        ownerId = ins.readInt();
        name = (ins.readField(String) as String);
        items = (ins.readObject() as TypedArray);
    }

    // from interface Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeInt(listId);
        out.writeInt(ownerId);
        out.writeField(name);
        out.writeObject(items);
    }
}
}
