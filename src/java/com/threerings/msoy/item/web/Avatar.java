//
// $Id$

package com.threerings.msoy.item.web;

/**
 * Represents an avatar that's usable in the msoy system.
 */
public class Avatar extends Item
{
    /** Identifies our headshot media. */
    public static final String HEADSHOT_MEDIA = "headshot";

    /** The avatar media. */
    public MediaDesc avatarMedia;

    /** The headshot media. */
    public MediaDesc headShotMedia;

    /**
     * Returns a {@link MediaDesc} configured to display the default non-guest avatar.
     */
    public static MediaDesc getDefaultMemberAvatarMedia ()
    {
        return new StaticMediaDesc(MediaDesc.APPLICATION_SHOCKWAVE_FLASH, AVATAR, "member");
    }

    /**
     * Returns a {@link MediaDesc} configured to display the default guest avatar.
     */
    public static MediaDesc getDefaultGuestAvatarMedia ()
    {
        return new StaticMediaDesc(MediaDesc.APPLICATION_SHOCKWAVE_FLASH, AVATAR, "guest");
    }

    /**
     * Returns a {@link MediaDesc} configured to display our default headshot media.
     */
    public static MediaDesc getDefaultHeadshotMedia ()
    {
        return new StaticMediaDesc(MediaDesc.IMAGE_PNG, AVATAR, HEADSHOT_MEDIA);
    }

    // @Override // from Item
    public byte getType ()
    {
        return AVATAR;
    }

    /**
     * Returns a media descriptor for the media that should be used to display our headshot
     * representation.
     */
    public MediaDesc getHeadShotMedia ()
    {
        return (headShotMedia != null) ? headShotMedia : getDefaultHeadshotMedia();
    }

    // @Override // from Item
    public MediaDesc getPreviewMedia ()
    {
        return getFurniMedia();
    }

    // @Override // from Item
    public boolean isConsistent ()
    {
        return super.isConsistent() && (avatarMedia != null);
    }

    // @Override // from Item
    protected MediaDesc getDefaultThumbnailMedia ()
    {
        if (headShotMedia != null && headShotMedia.isImage()) {
            return headShotMedia;
        }
        return super.getDefaultThumbnailMedia();
    }

    // @Override // from Item
    protected MediaDesc getDefaultFurniMedia ()
    {
        return avatarMedia;
    }
}
