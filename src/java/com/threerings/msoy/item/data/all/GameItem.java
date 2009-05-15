//
// $Id$

package com.threerings.msoy.item.data.all;

/**
 * A base class for items that are associated with a game.
 */
public abstract class GameItem extends Item
{
    /** The game to which this item belongs. */
    public int gameId;
}
