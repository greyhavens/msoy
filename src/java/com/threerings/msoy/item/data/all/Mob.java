//
// $Id$

package com.threerings.msoy.item.data.all;

/**
 * Contains the runtime data for a Mob (mobile-object, monster) item.
 */
public class Mob extends SubItem
{
    /** The id of the game with which we're associated. */
    public int gameId;

    // @Override // from Item
    public byte getType ()
    {
        return MOB;
    }

    // @Override // from Item
    public byte getSuiteMasterType ()
    {
        return GAME;
    }

    // @Override
    public boolean isConsistent ()
    {
        return super.isConsistent() && (furniMedia != null) && nonBlank(name, MAX_NAME_LENGTH);
    }

    // @Override // from Item
    public MediaDesc getPreviewMedia ()
    {
        return getFurniMedia();
    }

    // @Override // from Item
    protected MediaDesc getDefaultFurniMedia ()
    {
        return null; // there is no default
    }
}
