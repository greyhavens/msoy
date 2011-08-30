//
// $Id$

package com.threerings.msoy.item.data.all {
import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

import com.threerings.util.ByteEnum;
import com.threerings.util.Comparable;
import com.threerings.util.Hashable;

/**
 * A fully qualified item identifier (type and integer id).
 */
public class ItemIdent
    implements Comparable, Streamable, Hashable
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

    public static function fromString (entityId :String) :ItemIdent
    {
        var tokens :Array = entityId.split(":");

        return new ItemIdent(tokens[0], tokens[1]);
    }

    // from Equalable
    public function hashCode () :int
    {
        return (type * 37) | itemId;
    }

    // from Comparable
    public function compareTo (other :Object) :int
    {
        var that :ItemIdent = (other as ItemIdent);

        // first, compare by type.
        if (this.type < that.type) {
            return 1;

        } else if (this.type > that.type) {
            return -1;

        } else {
            // if type is equal, compare by item id
            if (this.itemId < that.itemId) {
                return 1;

            } else if (this.itemId > that.itemId) {
                return -1;

            } else {
                return 0;
            }
        }
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
        type = MsoyItemType(ins.readObject()).toByte();
        itemId = ins.readInt();
    }

    // from interface Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeObject(ByteEnum.fromByte(MsoyItemType, type));
        out.writeInt(itemId);
    }
}
}
