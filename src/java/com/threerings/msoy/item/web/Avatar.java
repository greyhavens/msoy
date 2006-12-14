//
// $Id$

package com.threerings.msoy.item.web;

/**
 * Represents an avatar that's usable in the msoy system.
 */
public class Avatar extends Item
{
    /** The avatar media. */
    public MediaDesc avatarMedia;

    /** The headshot media. */
    public MediaDesc headShotMedia;

    // @Override // from Item
    public byte getType ()
    {
        return AVATAR;
    }

    /**
     * Returns a media descriptor for the media that should be used
     * to display our headshot representation.
     */
    public MediaDesc getHeadShotMedia ()
    {
        return (headShotMedia != null) ? headShotMedia :
            new StaticMediaDesc(StaticMediaDesc.HEADSHOT, AVATAR);
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
        if (avatarMedia != null && avatarMedia.isImage()) {
            return avatarMedia;
        }
        return super.getDefaultThumbnailMedia();
    }

    // @Override // from Item
    protected MediaDesc getDefaultFurniMedia ()
    {
        return avatarMedia;
    }
}
