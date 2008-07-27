//
// $Id$

package com.threerings.msoy.item.data.all {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.msoy.data.all.MediaDesc;

/**
 * Represents an avatar that's usable in the msoy system.
 */
public class Avatar extends Item
{
    /** The avatar media. */
    public var avatarMedia :MediaDesc;

    /** The scaling to apply to the avatar media. */
    public var scale :Number;

    /**
     * Returns a {@link MediaDesc} configured to display the default non-guest avatar.
     */
    public static function getDefaultMemberAvatarMedia () :MediaDesc
    {
        return new DefaultItemMediaDesc(MediaDesc.APPLICATION_SHOCKWAVE_FLASH, AVATAR, "member");
    }

    /**
     * Returns a {@link MediaDesc} configured to display the default guest avatar.
     */
    public static function getDefaultGuestAvatarMedia () :MediaDesc
    {
        return new DefaultItemMediaDesc(MediaDesc.APPLICATION_SHOCKWAVE_FLASH, AVATAR, "guest");
    }

    public function Avatar ()
    {
    }

    // from Item
    override public function getPreviewMedia () :MediaDesc
    {
        return avatarMedia;
    }

    // from Item
    override public function getType () :int
    {
        return AVATAR;
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        avatarMedia = (ins.readObject() as MediaDesc);
        scale = ins.readFloat();
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeObject(avatarMedia);
        out.writeFloat(scale);
    }

    // from Item
    override protected function getDefaultFurniMedia () :MediaDesc
    {
        return avatarMedia;
    }
}
}
