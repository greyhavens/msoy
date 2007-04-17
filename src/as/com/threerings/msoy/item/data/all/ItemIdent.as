//
// $Id$

package com.threerings.msoy.item.data.all {

import com.threerings.util.Hashable;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

/**
 * A fully qualified item identifier (type and integer id).
 */
public class ItemIdent
    implements Hashable, Streamable
{
    /** The item type identifier. */
    public var type :int;

    /** The integer identifier of the item. */
    public var itemId :int;

    /**
     * Creates an identifier for the specified item.
     */
    public function ItemIdent (type :int = 0, itemId :int = 0)
    {
        this.type = type;
        this.itemId = itemId;
    }

    // from Equalable
    public function equals (other :Object) :Boolean
    {
        if (other is ItemIdent) {
            var that :ItemIdent = (other as ItemIdent);
            return (this.type == that.type) && (this.itemId == that.itemId);
        }
        return false;
    }

    // from Equalable
    public function hashCode () :int
    {
        return (type * 37) | itemId;
    }

    /**
     * Generates a string representation of this instance.
     */
    public function toString () :String
    {
        return type + ":" + itemId;
    }

    // from interface Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        type = ins.readByte();
        itemId = ins.readInt();
    }

    // from interface Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeByte(type);
        out.writeInt(itemId);
    }
}
}
