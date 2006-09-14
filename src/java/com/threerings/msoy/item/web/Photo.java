//
// $Id$

package com.threerings.msoy.item.web;

/**
 * Represents an uploaded photograph for display in albumns or for use as a
 * profile picture.
 */
public class Photo extends MediaItem
{
    /** A caption for this photo (max length 255 characters). */
    public String caption;

    // @Override // from Item
    public String getType ()
    {
        return "PHOTO";
    }

    // @Override // from Item
    public String getDescription ()
    {
        return caption;
    }

    // @Override // from Item
    public String getThumbnailPath ()
    {
        return getMediaPath();
    }

    // @Override
    public boolean isConsistent ()
    {
        return super.isConsistent() && nonBlank(caption);
    }
}
