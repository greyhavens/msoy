//
// $Id$

package com.threerings.msoy.item.web {

import flash.utils.ByteArray;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

/**
 * Represents an uploaded photograph for display in albumns or for use as a
 * profile picture.
 */
public class Photo extends Item
{
    /** A hash code identifying the photo media. */
    public var photoMediaHash :ByteArray;

    /** The MIME type of the {@link #photoMediaHash} media. */
    public var photoMimeType :int;

    /** A caption for this photo (max length 255 characters). */
    public var caption :String;

    /**
     * Returns a media descriptor for the actual photo media.
     */
    public function getPhotoMedia () :MediaDesc
    {
        return new MediaDesc(photoMediaHash, photoMimeType);
    }

    // from Item
    override public function getType () :String
    {
        return "PHOTO";
    }

    // from Item
    override public function getDescription () :String
    {
        return caption;
    }

    override protected function getDefaultThumbnailMedia () :MediaDesc
    {
        return getPhotoMedia();
    }

    override protected function getDefaultFurniMedia () :MediaDesc
    {
        return getPhotoMedia();
    }

    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        out.writeField(photoMediaHash);
        out.writeByte(photoMimeType);
        out.writeField(caption);
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        photoMediaHash = (ins.readField(ByteArray) as ByteArray);
        photoMimeType = ins.readByte();
        caption = (ins.readField(String) as String);
    }
}
}
