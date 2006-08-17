//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.util.MessageBundle;

import com.threerings.parlor.game.client.GameConfigurator;
import com.threerings.parlor.game.data.GameConfig;
import com.threerings.parlor.game.data.PartyGameConfig;

import com.threerings.msoy.data.MediaData;

/**
 * A game config for a simple multiplayer flash game.
 */
public class FlashGameConfig extends GameConfig
    implements PartyGameConfig
{
    /** A creator-submitted name of the game. */
    public String gameName;

    /** The media that is the game we're going to play. */
    public MediaData game;

    // from abstract GameConfig
    public String getBundleName ()
    {
        return "general";
    }

    // from abstract GameConfig
    public GameConfigurator createConfigurator ()
    {
        return null; // nothing here on the java side
    }

    @Override
    public String getGameName ()
    {
        return MessageBundle.taint(gameName);
    }

    // from abstract PlaceConfig
    public String getManagerClassName ()
    {
        return "com.threerings.msoy.game.server.FlashGameManager";
    }

    // from PartyGameConfig
    public byte getPartyGameType ()
    {
        // TODO
        return NOT_PARTY_GAME;
    }

    @Override
    public boolean equals (Object other)
    {
        if (!super.equals(other)) {
            return false;
        }

        FlashGameConfig that = (FlashGameConfig) other;
        return this.gameName.equals(that.gameName) && 
            this.game.equals(that.game);
    }

    @Override
    public int hashCode ()
    {
        return super.hashCode() + game.hashCode();
    }
}
