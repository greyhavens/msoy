//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.ezgame.data.EZGameConfig;

/**
 * A game config for a metasoy game.
 */
public class MsoyGameConfig extends EZGameConfig
{
    /** The name of our game. */
    public String name;

    @Override // documentation inherited
    public String getManagerClassName ()
    {
        return "com.threerings.msoy.game.server.MsoyGameManager";
    }
}
