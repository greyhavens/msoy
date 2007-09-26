//
// $Id$

package com.threerings.msoy.item.data.all {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.msoy.item.data.all.MediaDesc;

/**
 * Contains the runtime data for a LevelPack item.
 */
public class LevelPack extends Item
{
    /** The level media. */
    public var levelMedia :MediaDesc;

    public function LevelPack ()
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
        return LEVEL_PACK;
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        levelMedia = (ins.readObject() as MediaDesc);
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeObject(levelMedia);
    }
}
}
