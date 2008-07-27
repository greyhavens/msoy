//
// $Id$

package com.threerings.msoy.item.data.all {

import com.threerings.msoy.data.all.MediaDesc;

/**
 * Contains the runtime data for an ItemPack item.
 */
public class ItemPack extends SubItem
{
    public function ItemPack ()
    {
    }

    // from Item
    override public function getPreviewMedia () :MediaDesc
    {
        return getThumbnailMedia();
    }

    // from Item
    override public function getType () :int
    {
        return ITEM_PACK;
    }
}
}
