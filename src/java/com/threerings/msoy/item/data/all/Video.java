//
// $Id$

package com.threerings.msoy.item.data.all;

/**
 * Represents video data.
 */
public class Video extends Item
{
    /** The video media.*/
    public MediaDesc videoMedia;

    // @Override // from Item
    public byte getType ()
    {
        return VIDEO;
    }

    // @Override // from Item
    public boolean isConsistent ()
    {
        return super.isConsistent() && (videoMedia != null) && videoMedia.isVideo() &&
            nonBlank(name);
    }

    // @Override // from Item
    public MediaDesc getPreviewMedia ()
    {
        return getThumbnailMedia(); // TODO: support preview image
    }
}
