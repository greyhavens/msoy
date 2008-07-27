//
// $Id$

package com.threerings.msoy.item.data.all;

import com.threerings.msoy.data.all.MediaDesc;

/**
 * Contains the runtime data for a LevelPack item.
 */
public class LevelPack extends SubItem
{
    /** Premium level packs must be purchased to be used. */
    public boolean premium;

    @Override // from Item
    public byte getType ()
    {
        return LEVEL_PACK;
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
