//
// $Id$

package com.threerings.msoy.item.data.all;

import com.threerings.msoy.data.all.ConstrainedMediaDesc;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MediaMimeTypes;

/**
 * Represents an avatar that's usable in the msoy system.
 */
public class Avatar extends Item
{
    /**
     * When a player's avatar is updated, we also want to be told whether or not that avatar
     * is suitable for the player's current theme (this saves us pointless database lookups and
     * invoker round trips).
     */
    public enum QuicklistState {
        /** The avatar is valid for the theme, it should be in the player's quicklist. */
        VALID,
        /** The avatar is invalid for the theme; remove it from the player's quicklist. */
        INVALID,
        /** Keep the avatar in the quicklist if it's there, but don't add it. */
        DONT_TOUCH
    }

    /** The avatar media. */
    public ConstrainedMediaDesc avatarMedia;

    /** The scaling to apply to the avatar media. */
    public float scale;

    /**
     * Returns a <code>MediaDesc</code> configured to display the default non-guest avatar.
     */
    public static MediaDesc getDefaultMemberAvatarMedia ()
    {
        return new DefaultItemMediaDesc(
                MediaMimeTypes.APPLICATION_SHOCKWAVE_FLASH, MsoyItemType.AVATAR, "member");
    }

    /**
     * Returns a <code>MediaDesc</code> configured to display the default guest avatar.
     */
    public static MediaDesc getDefaultGuestAvatarMedia ()
    {
        return new DefaultItemMediaDesc(
                MediaMimeTypes.APPLICATION_SHOCKWAVE_FLASH, MsoyItemType.AVATAR, "guest");
    }

    /**
     * Returns a <code>MediaDesc</code> configured to display an avatar as a static image.
     */
    public static MediaDesc getStaticImageAvatarMedia ()
    {
        return new DefaultItemMediaDesc(MediaMimeTypes.IMAGE_PNG, MsoyItemType.AVATAR, "static");
    }

    @Override // from Item
    public MsoyItemType getType ()
    {
        return MsoyItemType.AVATAR;
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
            (avatarMedia.isSWF() || avatarMedia.isRemixed());
    }

    @Override // from Item
    public ConstrainedMediaDesc getPrimaryMedia ()
    {
        return avatarMedia;
    }

    @Override // from Item
    public void setPrimaryMedia (ConstrainedMediaDesc desc)
    {
        avatarMedia = desc;
    }

    @Override // from Item
    protected MediaDesc getDefaultFurniMedia ()
    {
        return avatarMedia;
    }
}
