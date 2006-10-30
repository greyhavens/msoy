//
// $Id$

package com.threerings.msoy.item.web;

/**
 * Represents an uploaded photograph for display in albumns or for use as a
 * profile picture.
 */
public class Photo extends Item
{
    /** The photo media. */
    public MediaDesc photoMedia;

    /** A caption for this photo (max length 255 characters). */
    public String caption;

    // @Override // from Item
    public byte getType ()
    {
        return PHOTO;
    }

    // @Override // from Item
    public String getDescription ()
    {
        return caption;
    }

    // @Override // from Item
    public boolean isConsistent ()
    {
        return super.isConsistent() && (photoMedia != null) &&
            nonBlank(caption);
    }

    // @Override // from Item
    protected MediaDesc getDefaultThumbnailMedia ()
    {
        if (photoMedia != null && photoMedia.isImage()) {
            return photoMedia;
        }
        return super.getDefaultThumbnailMedia();
    }

    // @Override // from Item
    protected MediaDesc getDefaultFurniMedia ()
    {
        return photoMedia;
    }
}
