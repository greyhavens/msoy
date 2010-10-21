//
// $Id$

package com.threerings.msoy.item.data.all;

import com.threerings.orth.data.MediaDesc;

/**
 * Contains the runtime data for a LevelPack item.
 */
public class LevelPack extends IdentGameItem
{
    /** Premium level packs must be purchased to be used. */
    public boolean premium;

    @Override // from Item
    public MsoyItemType getType ()
    {
        return MsoyItemType.LEVEL_PACK;
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
