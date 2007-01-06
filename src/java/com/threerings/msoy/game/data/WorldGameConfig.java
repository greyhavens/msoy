//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.ezgame.data.EZGameConfig;

import com.threerings.msoy.item.web.Game;

/**
 * A game config for an in-world game.
 */
public class WorldGameConfig extends EZGameConfig
{
    /** The game item. */
    public Game game;
    
    @Override // documentation inherited
    public String getManagerClassName ()
    {
        return "com.threerings.msoy.game.server.WorldGameManager";
    }
}
