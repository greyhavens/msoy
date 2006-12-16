//
// $Id$

package com.threerings.msoy.item.web {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

/**
 * Represents a pet, which is just furniture, really.
 */
public class Pet extends Item
{
    // from Item
    override public function getType () :int
    {
        return PET;
    }

    override protected function getDefaultThumbnailMedia () :MediaDesc
    {
        if (furniMedia != null && furniMedia.isImage()) {
            return furniMedia;
        }
        return super.getDefaultThumbnailMedia();
    }

    override protected function getDefaultFurniMedia () :MediaDesc
    {
        return null; // there is no default
    }

    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        // nada for now
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        // nada for now
    }
}
}
