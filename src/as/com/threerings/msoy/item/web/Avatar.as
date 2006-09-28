//
// $Id$

package com.threerings.msoy.item.web {

import flash.utils.ByteArray;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

/**
 * Represents an avatar that's usable in the msoy system.
 */
public class Avatar extends Item
{
    /** A hash code identifying the avatar media. */
    public var avatarMediaHash :ByteArray;

    /** The MIME type of the {@link #avatarMediaHash} media. */
    public var avatarMimeType :int;

    /** A description for this avatar (max length 255 characters). */
    public var description :String;

    /**
     * Returns a media descriptor for the actual avatar media.
     */
    public function getAvatarMedia () :MediaDesc
    {
        return new MediaDesc(avatarMediaHash, avatarMimeType);
    }

    // from Item
    override public function getType () :String
    {
        return "AVATAR";
    }

    // from Item
    override public function getDescription () :String
    {
        return description;
    }

    override protected function getDefaultThumbnailMedia () :MediaDesc
    {
        return getAvatarMedia();
    }

    override protected function getDefaultFurniMedia () :MediaDesc
    {
        return getAvatarMedia();
    }

    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        out.writeField(avatarMediaHash);
        out.writeByte(avatarMimeType);
        out.writeField(description);
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        avatarMediaHash = (ins.readField(ByteArray) as ByteArray);
        avatarMimeType = ins.readByte();
        description = (ins.readField(String) as String);
    }
}
}
