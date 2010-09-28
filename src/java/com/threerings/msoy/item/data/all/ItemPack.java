//
// $Id$

package com.threerings.msoy.item.data.all;

import com.threerings.msoy.data.all.MediaDesc;

/**
 * Contains the runtime data for an ItemPack item.
 */
public class ItemPack extends IdentGameItem
{
    @Override // from Item
    public MsoyItemType getType ()
    {
        return MsoyItemType.ITEM_PACK;
    }

    @Override // from Item
    public MediaDesc getPreviewMedia ()
    {
        return getThumbnailMedia();
    }

    @Override // from Item
    public boolean isConsistent ()
    {
        return super.isConsistent() && nonBlank(name, MAX_NAME_LENGTH) && (_furniMedia != null);
    }
}
