//
// $Id$

package com.threerings.msoy.ui {

import mx.containers.VBox;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;

/**
 * Displays an item thumbnail image.
 */
public class ThumbnailPanel extends VBox
{
    public function ThumbnailPanel (size :int = MediaDesc.THUMBNAIL_SIZE)
    {
        styleName = "thumbnailPanel";
        _size = size;
    }

    public function setItem (item :Item) :void
    {
        setMediaDesc(item.getThumbnailMedia());
    }

    public function setMediaDesc (media :MediaDesc) :void
    {
        while (numChildren > 0) {
            removeChildAt(0);
        }
        addChild(MediaWrapper.createScaled(
                     media, MediaDesc.DIMENSIONS[2*_size], MediaDesc.DIMENSIONS[2*_size+1]))
    }

    protected var _size :int;
}
}
