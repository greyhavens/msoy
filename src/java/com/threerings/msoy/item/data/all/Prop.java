//
// $Id$

package com.threerings.msoy.item.data.all;

import com.threerings.msoy.data.all.MediaDesc;

/**
 * Contains the runtime data for a Prop item. A prop is smart furniture that is associated with an
 * AVRG.
 */
public class Prop extends SubItem
{
    /** The id of the game with which we're associated. */
    public int gameId;

    @Override // from Item
    public byte getType ()
    {
        return PROP;
    }

    @Override // from Item
    public byte getSuiteMasterType ()
    {
        return GAME;
    }

    @Override
    public boolean isConsistent ()
    {
        return super.isConsistent() && nonBlank(name, MAX_NAME_LENGTH) && (furniMedia != null);
    }

    @Override // from Item
    public MediaDesc getPreviewMedia ()
    {
        return getFurniMedia();
    }

    @Override // from Item
    protected MediaDesc getDefaultFurniMedia ()
    {
        return null; // there is no default
    }
}
