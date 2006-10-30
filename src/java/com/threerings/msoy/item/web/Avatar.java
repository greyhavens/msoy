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

    /** A description for this avatar (max length 255 characters). */
    public String description;

    // @Override // from Item
    public byte getType ()
    {
        return AVATAR;
    }

    // @Override // from Item
    public String getDescription ()
    {
        return description;
    }

    // @Override // from Item
    public boolean isConsistent ()
    {
        return super.isConsistent() && (avatarMedia != null) &&
            nonBlank(description);
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
