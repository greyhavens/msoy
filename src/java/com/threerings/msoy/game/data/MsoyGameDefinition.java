//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.toybox.data.GameDefinition;

/**
 * Customizes the standard {@link GameDefinition} for MSOY which mainly means
 * looking for our game jar files using a different naming scheme.
 */
public class MsoyGameDefinition extends GameDefinition
{
    @Override // from GameDefinition
    public String getJarName (int gameId)
    {
        return digest + ".jar";
    }
}
