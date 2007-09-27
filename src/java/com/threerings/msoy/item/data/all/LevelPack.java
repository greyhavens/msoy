//
// $Id$

package com.threerings.msoy.item.data.all;

/**
 * Contains the runtime data for a LevelPack item.
 */
public class LevelPack extends Item
{
    /** An identifier for this level pack, used by the game code. */
    public String ident;

    /** Premium level packs must be purchased to be used. */
    public boolean premium;

    // @Override // from Item
    public byte getType ()
    {
        return LEVEL_PACK;
    }

    // @Override // from Item
    public MediaDesc getPreviewMedia ()
    {
        return getThumbnailMedia();
    }

    // @Override // from Item
    public boolean isConsistent ()
    {
        return super.isConsistent() && nonBlank(name) && (furniMedia != null);
    }
}
