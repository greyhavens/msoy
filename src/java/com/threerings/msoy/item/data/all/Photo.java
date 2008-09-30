//
// $Id$

package com.threerings.msoy.item.data.all;

import com.threerings.msoy.data.all.MediaDesc;

/**
 * Represents an uploaded photograph for display in albumns or for use as a
 * profile picture.
 */
public class Photo extends Item
{
    /** The photo media. This is the full-size version. furniMedia is 4x thumbnail size. */
    public MediaDesc photoMedia;

    /** The width (in pixels) of the photo media. */
    public int photoWidth;

    /** The height (in pixels) of the photo media. */
    public int photoHeight;

    @Override // from Item
    public byte getType ()
    {
        return PHOTO;
    }

    @Override // from Item
    public boolean isConsistent ()
    {
        return super.isConsistent() && (photoMedia != null);
    }

    @Override // from Item
    public MediaDesc getPreviewMedia ()
    {
        return (furniMedia != null) ? furniMedia : thumbMedia;
    }

    @Override // from Item
    public MediaDesc getPrimaryMedia ()
    {
        return photoMedia;
    }

    @Override // from Item
    public void setPrimaryMedia (MediaDesc desc)
    {
        photoMedia = desc;
    }

    @Override// from Item
    protected MediaDesc getDefaultFurniMedia ()
    {
        return photoMedia;
    }
}
