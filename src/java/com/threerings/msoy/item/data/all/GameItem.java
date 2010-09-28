//
// $Id$

package com.threerings.msoy.item.data.all;

/**
 * A base class for items that are associated with a game.
 */
public abstract class GameItem extends Item
{
    /** All of the game item types, enumerated here in game editor tab order. */
    public static final MsoyItemType[] TYPES = {
            MsoyItemType.LAUNCHER, MsoyItemType.TROPHY_SOURCE, MsoyItemType.ITEM_PACK,
            MsoyItemType.LEVEL_PACK, MsoyItemType.PRIZE };

    /** The game to which this item belongs. */
    public int gameId;
}
