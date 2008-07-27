//
// $Id$

package com.threerings.msoy.item.data.all {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.msoy.data.all.StaticMediaDesc;

/**
 * Provides an item's default media.
 */
public class DefaultItemMediaDesc extends StaticMediaDesc
{
    public function DefaultItemMediaDesc (
        mimeType :int = 0, itemType :int = ItemTypes.NOT_A_TYPE, mediaType :String = null,
        constraint :int = NOT_CONSTRAINED)
    {
        super(mimeType, itemType == ItemTypes.NOT_A_TYPE ? null : Item.getTypeName(itemType),
              mediaType, constraint);
        _itemTypeCode = itemType;
    }

    // documentation inherited from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        _itemTypeCode = ins.readByte();
    }

    // documentation inherited from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeByte(_itemTypeCode);
    }

    protected var _itemTypeCode :int;
}
}
