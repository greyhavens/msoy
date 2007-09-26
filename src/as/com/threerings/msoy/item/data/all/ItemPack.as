//
// $Id$

package com.threerings.msoy.item.data.all {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.msoy.item.data.all.MediaDesc;

/**
 * Contains the runtime data for an ItemPack item.
 */
public class ItemPack extends Item
{
    /** The item media. */
    public var itemMedia :MediaDesc;

    public function ItemPack ()
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
        return ITEM_PACK;
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        itemMedia = (ins.readObject() as MediaDesc);
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeObject(itemMedia);
    }
}
}
