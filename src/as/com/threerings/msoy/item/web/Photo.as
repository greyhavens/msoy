//
// $Id$

package com.threerings.msoy.item.web {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

/**
 * Represents an uploaded photograph for display in albumns or for use as a
 * profile picture.
 */
public class Photo extends Item
{
    /** The photo media. */
    public var photoMedia :MediaDesc;

    // from Item
    override public function getType () :int
    {
        return PHOTO;
    }

    override protected function getDefaultThumbnailMedia () :MediaDesc
    {
        if (photoMedia != null && photoMedia.isImage()) {
            return photoMedia;
        }
        return super.getDefaultThumbnailMedia();
    }

    override protected function getDefaultFurniMedia () :MediaDesc
    {
        return photoMedia;
    }

    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        out.writeObject(photoMedia);
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        photoMedia = (ins.readObject() as MediaDesc);
    }
}
}
