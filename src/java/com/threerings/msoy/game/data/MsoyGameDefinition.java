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

    /** TEMP: hack to allow us to only run the agent for multiplayer games. */
    public boolean isAgentMPOnly;

    /** Indicates that this AVRG should not auto-send player into a room. */
    public boolean roomless;

    /** We need this here to be able to communicate with the whirled code that will launch the
     *  agent on the server. */
    public String serverMedia;

    /** Indicates the id of the bureau that this game's server code will run in, if any. */
    public String bureauId;

    /** The maximum client width to allow. */
    public int maxWidth;

    /** The maximum client height to allow. */
    public int maxHeight;

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

    /**
     * Configures the path to this game's server media.
     */
    public void setServerMediaPath (String mediaPath)
    {
        serverMedia = mediaPath;
    }

    @Override // from GameDefinition
    public String getServerMediaPath (int gameId)
    {
        // TODO: what are we supposed to do with gameId?
        return serverMedia;
    }

    /**
     * Configures the bureau id for this game's server code.
     */
    public void setBureauId (String id)
    {
        bureauId = id;
    }

    @Override // from GameDefinition
    public String getBureauId (int gameId)
    {
        return bureauId;
    }
}
