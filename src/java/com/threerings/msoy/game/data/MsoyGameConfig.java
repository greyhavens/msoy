//
// $Id$

package com.threerings.msoy.game.data;

import com.whirled.game.data.GameDefinition;

/**
 * An interface implemented by both Parlor and AVR games in Whirled.
 */
@com.threerings.util.ActionScript(omit=true)
public interface MsoyGameConfig
{
    /** Returns the id of this game. */
    public int getGameId ();

    /** Returns the non-changing metadata that defines this game. */
    public GameDefinition getGameDefinition ();
}
