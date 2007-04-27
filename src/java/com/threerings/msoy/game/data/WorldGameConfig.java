//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.util.ActionScript;

import com.threerings.msoy.item.data.all.Game;

/**
 * A game config for an in-world game.
 */
public class WorldGameConfig extends MsoyGameConfig
{
    /** The scene id in which the game was started. */
    public int startSceneId;

    @Override @ActionScript(omit=true)
    public String getManagerClassName ()
    {
        if (manager == null) {
            return "com.threerings.msoy.game.server.AVRGameManager";
        } else {
            return super.getManagerClassName();
        }
    }
}
