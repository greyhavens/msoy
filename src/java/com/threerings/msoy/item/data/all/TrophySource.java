//
// $Id$

package com.threerings.msoy.item.data.all;

/**
 * Contains the runtime data for a TrophySource item.
 */
public class TrophySource extends Item
{
    /** The required width for a trophy image. */
    public static final int TROPHY_WIDTH = 60;

    /** The required height for a trophy image. */
    public static final int TROPHY_HEIGHT = 60;

    /** An identifier for this trophy, used by the game code. */
    public String ident;

    // @Override // from Item
    public byte getType ()
    {
        return TROPHY_SOURCE;
    }

    // @Override // from Item
    public byte getSuiteMasterType ()
    {
        return GAME;
    }

    // @Override // from Item
    public MediaDesc getPreviewMedia ()
    {
        return getThumbnailMedia();
    }

    // @Override // from Item
    public boolean isConsistent ()
    {
        return super.isConsistent() && nonBlank(name, MAX_NAME_LENGTH) &&
            nonBlank(ident, Game.MAX_IDENT_LENGTH) && (thumbMedia != null);
    }
}
