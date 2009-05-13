//
// $Id$

package com.threerings.msoy.item.data.all;

import com.threerings.msoy.data.all.MediaDesc;

/**
 * Extends Item with game info.
 */
public class Game extends Item
{
    /** A unique identifier assigned to this game and preserved across new versions of the game
     * item so that ratings and lobbies and content packs all reference the same "game". */
    public int gameId;

    @Override // from Item
    public byte getType ()
    {
        return GAME;
    }

    @Override // from Item
    public MediaDesc getPrimaryMedia ()
    {
        return getFurniMedia();
    }

    @Override // from Item
    public MediaDesc getPreviewMedia ()
    {
        return getFurniMedia();
    }

    @Override
    public boolean isConsistent ()
    {
        return super.isConsistent() && nonBlank(name, MAX_NAME_LENGTH) && (_furniMedia != null) &&
            (_furniMedia.hasFlashVisual() || _furniMedia.isRemixed());
    }
}
