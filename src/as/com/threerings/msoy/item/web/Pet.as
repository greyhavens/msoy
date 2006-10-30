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
    /** A description of this pet (max length 255 characters). */
    public var description :String;

    // from Item
    override public function getType () :int
    {
        return PET;
    }

    // from Item
    override public function getDescription () :String
    {
        return description;
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

        out.writeField(description);
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        description = (ins.readField(String) as String);
    }
}
}
