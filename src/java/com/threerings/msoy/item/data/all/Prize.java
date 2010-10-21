//
// $Id$

package com.threerings.msoy.item.data.all;

import com.threerings.orth.data.MediaDesc;

/**
 * Contains the runtime data for a Prize item.
 */
public class Prize extends IdentGameItem
{
    /** The item type of the target prize item. */
    public MsoyItemType targetType;

    /** The catalog id of the target prize item's listing. */
    public int targetCatalogId;

    @Override // from Item
    public MsoyItemType getType ()
    {
        return MsoyItemType.PRIZE;
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
        return super.isConsistent() && (targetType != MsoyItemType.NOT_A_TYPE) &&
            (targetCatalogId != 0);
    }

    @Override // from IdentGameItem
    public boolean isSalable ()
    {
        return false;
    }
}
