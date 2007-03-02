//
// $Id$

package com.threerings.msoy.game.chiyogami.data;

import com.threerings.parlor.game.client.GameConfigurator;
import com.threerings.parlor.game.data.GameConfig;

/**
 * A GameConfig for Chiyogami dance battle.
 */
public class ChiyogamiConfig extends GameConfig
{
    @Override
    public byte getGameType ()
    {
        return PARTY;
    }

    // from GameConfig
    public String getBundleName ()
    {
        return "chiyogami";
    }

    // from GameConfig
    public GameConfigurator createConfigurator ()
    {
        return null; // unneeded here
    }

    // from PlaceConfig
    public String getManagerClassName ()
    {
        return "com.threerings.msoy.game.chiyogami.server.ChiyogamiManager";
    }
}
