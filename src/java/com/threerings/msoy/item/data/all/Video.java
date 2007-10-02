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
            nonBlank(name, MAX_NAME_LENGTH);
    }

    // @Override // from Item
    public MediaDesc getPreviewMedia ()
    {
        if (videoMedia != null && videoMedia.isExternalVideo()) {
            return videoMedia;
        }
        return getThumbnailMedia(); // TODO: support preview image
    }

    // @Override // from Item
    protected MediaDesc getDefaultFurniMedia ()
    {
        if (videoMedia != null && videoMedia.isExternalVideo()) {
            return videoMedia;
        }

        // else: TODO
        return super.getDefaultFurniMedia();
    }
}
