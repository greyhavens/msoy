//
// $Id$

package com.threerings.msoy.item.web;

/**
 * Represents an avatar that's usable in the msoy system.
 */
public class Avatar extends Item
{
    /** A hash code indentifying the avatar media. */
    public byte[] avatarMediaHash;

    /** The MIME typeof the {@link #avatarMediaHash} media. */
    public byte avatarMimeType;

    /** A description for this avatar (max length 255 characters). */
    public String description;

    /**
     * Returns a media descriptor for the actual avatar media.
     */
    public MediaDesc getAvatarMedia ()
    {
        return new MediaDesc(avatarMediaHash, avatarMimeType);
    }

    // @Override // from Item
    public String getType ()
    {
        return "AVATAR";
    }

    // @Override // from Item
    public String getDescription ()
    {
        return description;
    }

    // @Override // from Item
    public boolean isConsistent ()
    {
        return super.isConsistent() && (avatarMediaHash != null) &&
            nonBlank(description);
    }

    // @Override // from Item
    protected MediaDesc getDefaultThumbnailMedia ()
    {
        return getAvatarMedia();
    }

    // @Override // from Item
    protected MediaDesc getDefaultFurniMedia ()
    {
        return getAvatarMedia();
    }
}
