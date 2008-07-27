//
// $Id$

package com.threerings.msoy.item.data.all;

import com.threerings.msoy.data.all.MediaDesc;

/**
 * Contains the runtime data for an ItemPack item.
 */
public class ItemPack extends SubItem
{
    @Override // from Item
    public byte getType ()
    {
        return ITEM_PACK;
    }

    @Override // from Item
    public byte getSuiteMasterType ()
    {
        return GAME;
    }

    @Override // from Item
    public MediaDesc getPreviewMedia ()
    {
        return getThumbnailMedia();
    }

    @Override // from Item
    public boolean isConsistent ()
    {
        return super.isConsistent() && nonBlank(name, MAX_NAME_LENGTH) && (furniMedia != null);
    }
}
