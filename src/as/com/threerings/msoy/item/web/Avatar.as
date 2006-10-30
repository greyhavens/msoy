//
// $Id$

package com.threerings.msoy.item.web {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

/**
 * Represents an avatar that's usable in the msoy system.
 */
public class Avatar extends Item
{
    /** The avatar media. */
    public var avatarMedia :MediaDesc;

    /** A description for this avatar (max length 255 characters). */
    public var description :String;

    // from Item
    override public function getType () :int
    {
        return AVATAR;
    }

    // from Item
    override public function getDescription () :String
    {
        return description;
    }

    override protected function getDefaultThumbnailMedia () :MediaDesc
    {
        if (avatarMedia != null && avatarMedia.isImage()) {
            return avatarMedia;
        }
        return super.getDefaultThumbnailMedia();
    }

    override protected function getDefaultFurniMedia () :MediaDesc
    {
        return avatarMedia;
    }

    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        out.writeObject(avatarMedia);
        out.writeField(description);
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        avatarMedia = (ins.readObject() as MediaDesc);
        description = (ins.readField(String) as String);
    }
}
}
