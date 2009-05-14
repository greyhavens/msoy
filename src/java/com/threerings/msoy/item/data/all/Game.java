//
// $Id$

package com.threerings.msoy.item.data.all;

import com.threerings.msoy.data.all.MediaDesc;

/**
 * Extends Item with game info.
 */
public class Game extends Item
{
    /** A unique identifier assigned to this game. */
    public int gameId;

    /** True if this is an AVR game, false if it's a Parlor game. */
    public boolean isAVRG;

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
