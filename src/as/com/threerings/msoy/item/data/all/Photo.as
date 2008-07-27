//
// $Id$

package com.threerings.msoy.item.data.all {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.msoy.data.all.MediaDesc;

/**
 * Represents an uploaded photograph for display in albumns or for use as a
 * profile picture.
 */
public class Photo extends Item
{
    /** The photo media. */
    public var photoMedia :MediaDesc;

    /** The width (in pixels) of the photo media. */
    public var photoWidth :int;

    /** The height (in pixels) of the photo media. */
    public var photoHeight :int;

    public function Photo ()
    {
    }

    // from Item
    override public function getPreviewMedia () :MediaDesc
    {
        return photoMedia;
    }

    // from Item
    override public function getType () :int
    {
        return PHOTO;
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        photoMedia = (ins.readObject() as MediaDesc);
        photoWidth = ins.readInt();
        photoHeight = ins.readInt();
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeObject(photoMedia);
        out.writeInt(photoWidth);
        out.writeInt(photoHeight);
    }

    // from Item
    override protected function getDefaultFurniMedia () :MediaDesc
    {
        return photoMedia;
    }
}
}
