//
// $Id$

package com.threerings.msoy.item.data.all;

import com.threerings.msoy.data.all.MediaDesc;

/**
 * Contains the runtime data for a Prize item.
 */
public class Prize extends SubItem
{
    /** The item type of the target prize item. */
    public byte targetType;

    /** The catalog id of the target prize item's listing. */
    public int targetCatalogId;

    @Override // from Item
    public byte getType ()
    {
        return PRIZE;
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
    public MediaDesc getPrimaryMedia ()
    {
        return getThumbnailMedia();
    }

    @Override // from Item
    public MediaDesc getThumbnailMedia ()
    {
        return getDefaultThumbnailMediaFor(targetType);
    }

    @Override // from Item
    public boolean isConsistent ()
    {
        return super.isConsistent() && (targetType != Item.NOT_A_TYPE) &&
            (targetCatalogId != 0);
    }

    @Override // from SubItem
    public boolean isSalable ()
    {
        return false;
    }
}
