//
// $Id$

package com.threerings.msoy.item.data.all {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.msoy.data.all.MediaDesc;

/**
 * Contains the runtime data for a Prize item.
 */
public class Prize extends SubItem
{
    /** The item type of the target prize item. */
    public var targetType :int;

    /** The catalog id of the target prize item's listing. */
    public var targetCatalogId :int;

    public function Prize ()
    {
    }

    // from Item
    override public function getPreviewMedia () :MediaDesc
    {
        return getThumbnailMedia();
    }

    // from Item
    override public function getType () :int
    {
        return PRIZE;
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        targetType = ins.readByte();
        targetCatalogId = ins.readInt();
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeByte(targetType);
        out.writeInt(targetCatalogId);
    }
}
}
