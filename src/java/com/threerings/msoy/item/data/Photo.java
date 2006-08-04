//
// $Id$

package com.threerings.msoy.item.data;

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
    public String getInventoryDescrip ()
    {
        return toInventoryDescrip(caption);
    }

    // @Override // from Item
    public String getThumbnailPath ()
    {
        return getMediaPath();
    }
}
