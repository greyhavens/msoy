//
// $Id$

package com.threerings.msoy.item.data.all;

import com.threerings.msoy.data.all.MediaDesc;

/**
 * Represents video data.
 */
public class Video extends Item
{
    /** The video media.*/
    public MediaDesc videoMedia;

    @Override // from Item
    public byte getType ()
    {
        return VIDEO;
    }

    @Override // from Item
    public boolean isConsistent ()
    {
        return super.isConsistent() && (videoMedia != null) && videoMedia.isVideo() &&
            nonBlank(name, MAX_NAME_LENGTH);
    }

    @Override // from Item
    public MediaDesc getPreviewMedia ()
    {
        if (videoMedia != null && videoMedia.isExternalVideo()) {
            return videoMedia;
        }
        return getThumbnailMedia(); // TODO: support preview image
    }

    @Override // from Item
    public MediaDesc getPrimaryMedia ()
    {
        return videoMedia;
    }

    @Override // from Item
    public void setPrimaryMedia (MediaDesc desc)
    {
        videoMedia = desc;
    }

    @Override // from Item
    protected MediaDesc getDefaultFurniMedia ()
    {
        if (videoMedia != null && videoMedia.isExternalVideo()) {
            return videoMedia;
        }

        // else: TODO
        return super.getDefaultFurniMedia();
    }
}
