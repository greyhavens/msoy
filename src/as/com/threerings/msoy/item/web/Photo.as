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
public class Photo extends MediaItem
{
    /** A hash code identifying the photo media. */
    public var photoMediaHash :ByteArray;

    /** The MIME type of the {@link #photoMediaHash} media. */
    public var photoMimeType :int;

    /** A caption for this photo (max length 255 characters). */
    public var caption :String;

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

    // from Item
    override public function getThumbnailPath () :String
    {
        // TODO: fixy fixy
        return getFurniMedia().getMediaPath();
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
