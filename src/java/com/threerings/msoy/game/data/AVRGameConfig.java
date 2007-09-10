//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.util.ActionScript;

/**
 * A game config for an AVR (in-world) game.
 */
public class AVRGameConfig extends MsoyGameConfig
{
    /** The scene id in which the game was started. */
    public int startSceneId;

    @Override @ActionScript(omit=true)
    public String getManagerClassName ()
    {
        if (getGameDefinition().manager == null) {
            return "com.threerings.msoy.game.server.AVRGameManager";
        } else {
            return super.getManagerClassName();
        }
    }
}
