//
// $Id$

package com.threerings.msoy.game.data;

import com.whirled.game.data.GameDefinition;

/**
 * Customizes the standard {@link GameDefinition} for MSOY which mainly means looking for our game
 * jar files using a different naming scheme.
 */
public class MsoyGameDefinition extends GameDefinition
{
    /** If true, the game requires the LWJGL libraries. */
    public boolean lwjgl;

    /**
     * Configures the path to this game's media.
     */
    public void setMediaPath (String mediaPath)
    {
        // we reuse the digest field for this as we don't otherwise use it in MSOY
        digest = mediaPath;
    }

    @Override // from GameDefinition
    public String getMediaPath (int gameId)
    {
        return digest;
    }
}
