//
// $Id$

package com.threerings.msoy.item.data.all;

/**
 * Contains the runtime data for a TrophySource item.
 */
public class TrophySource extends SubItem
{
    /** The required width for a trophy image. */
    public static final int TROPHY_WIDTH = 60;

    /** The required height for a trophy image. */
    public static final int TROPHY_HEIGHT = 60;

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
        return super.isConsistent() && nonBlank(name, MAX_NAME_LENGTH) && (thumbMedia != null);
    }

    // @Override // from SubItem
    public boolean isSalable ()
    {
        return false;
    }
}
