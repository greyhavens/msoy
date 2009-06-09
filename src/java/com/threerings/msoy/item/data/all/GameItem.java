//
// $Id$

package com.threerings.msoy.item.data.all;

/**
 * A base class for items that are associated with a game.
 */
public abstract class GameItem extends Item
{
    /** All of the game item types, enumerated here in game editor tab order. */
    public static final byte[] TYPES = { LAUNCHER, TROPHY_SOURCE, ITEM_PACK, LEVEL_PACK, PRIZE };

    /** The game to which this item belongs. */
    public int gameId;
}
