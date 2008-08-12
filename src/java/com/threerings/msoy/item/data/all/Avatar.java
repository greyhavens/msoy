//
// $Id$

package com.threerings.msoy.item.data.all;

import com.threerings.msoy.data.all.MediaDesc;

/**
 * Represents an avatar that's usable in the msoy system.
 */
public class Avatar extends Item
{
    /** The avatar media. */
    public MediaDesc avatarMedia;

    /** The scaling to apply to the avatar media. */
    public float scale;

    /**
     * Returns a {@link MediaDesc} configured to display the default non-guest avatar.
     */
    public static MediaDesc getDefaultMemberAvatarMedia ()
    {
        return new DefaultItemMediaDesc(MediaDesc.APPLICATION_SHOCKWAVE_FLASH, AVATAR, "member");
    }

    /**
     * Returns a {@link MediaDesc} configured to display the default guest avatar.
     */
    public static MediaDesc getDefaultGuestAvatarMedia ()
    {
        return new DefaultItemMediaDesc(MediaDesc.APPLICATION_SHOCKWAVE_FLASH, AVATAR, "guest");
    }

    @Override // from Item
    public byte getType ()
    {
        return AVATAR;
    }

    @Override // from Item
    public MediaDesc getPreviewMedia ()
    {
        return avatarMedia;
    }

    @Override // from Item
    public boolean isConsistent ()
    {
        return super.isConsistent() && nonBlank(name, MAX_NAME_LENGTH) && (avatarMedia != null) && 
            (avatarMedia.isSWF() || avatarMedia.isRemixable());
    }

    @Override // from Item
    public MediaDesc getPrimaryMedia ()
    {
        return avatarMedia;
    }

    @Override // from Item
    public void setPrimaryMedia (MediaDesc desc)
    {
        avatarMedia = desc;
    }

    @Override // from Item
    protected MediaDesc getDefaultFurniMedia ()
    {
        return avatarMedia;
    }
}
