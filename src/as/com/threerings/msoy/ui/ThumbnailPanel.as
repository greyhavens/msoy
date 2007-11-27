//
// $Id$

package com.threerings.msoy.ui {

import mx.containers.VBox;
import mx.core.ScrollPolicy;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;

/**
 * Displays an item thumbnail image.
 */
public class ThumbnailPanel extends VBox
{
    public function ThumbnailPanel ()
    {
        styleName = "thumbnailPanel";
        horizontalScrollPolicy = ScrollPolicy.OFF;
        verticalScrollPolicy = ScrollPolicy.OFF;
        width = MediaDesc.THUMBNAIL_WIDTH;
        height = MediaDesc.THUMBNAIL_HEIGHT;
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
        addChild(MediaWrapper.createScaled(media, width, height))
    }
}
}
