//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.ezgame.data.EZGameConfig;

/**
 * A game config for a simple multiplayer flash game.
 */
public class MsoyGameConfig extends EZGameConfig
{
    @Override // documentation inherited
    public String getManagerClassName ()
    {
        return "com.threerings.msoy.game.server.MsoyEZGameManager";
    }
}
