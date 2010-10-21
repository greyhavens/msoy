//
// $Id$

package com.threerings.msoy.item.data.all;

import com.threerings.msoy.data.all.ConstrainedMediaDesc;
import com.threerings.msoy.data.all.MediaDesc;

/**
 * Represents video data.
 */
public class Video extends Item
{
    /** The video media.*/
    public ConstrainedMediaDesc videoMedia;

    @Override // from Item
    public MsoyItemType getType ()
    {
        return MsoyItemType.VIDEO;
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
        // TODO: support preview image ??
        return videoMedia;
    }

    @Override // from Item
    public ConstrainedMediaDesc getPrimaryMedia ()
    {
        return videoMedia;
    }

    @Override // from Item
    public void setPrimaryMedia (ConstrainedMediaDesc desc)
    {
        videoMedia = desc;
    }
}
