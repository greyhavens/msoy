//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.ezgame.data.EZGameConfig;

import com.threerings.msoy.data.MediaData;

/**
 * A game config for a simple multiplayer flash game.
 */
public class FlashGameConfig extends EZGameConfig
{
    /** The media that is the game we're going to play. */
    public MediaData game;

    @Override
    public boolean equals (Object other)
    {
        if (!super.equals(other)) {
            return false;
        }

        FlashGameConfig that = (FlashGameConfig) other;
        return this.game.equals(that.game);
    }

    @Override
    public int hashCode ()
    {
        return super.hashCode() + game.hashCode();
    }
}
