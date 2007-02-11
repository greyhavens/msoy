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

    /**
     * Returns a {@link MediaDesc} configured to display the default non-guest avatar.
     */
    public static function getDefaultMemberAvatarMedia () :MediaDesc
    {
        return new StaticMediaDesc(MediaDesc.APPLICATION_SHOCKWAVE_FLASH, AVATAR, "member");
    }

    // from Item
    override public function getType () :int
    {
        return AVATAR;
    }

    override protected function getDefaultFurniMedia () :MediaDesc
    {
        return avatarMedia;
    }

    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        out.writeObject(avatarMedia);
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        avatarMedia = (ins.readObject() as MediaDesc);
    }
}
}
