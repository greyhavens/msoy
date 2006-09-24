//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.crowd.data.PlaceConfig;

import com.threerings.msoy.item.web.Game;

/**
 * The configuration data for a lobby object.
 */
public class LobbyConfig extends PlaceConfig
{
    /** The game item that is being played in this lobby. */
    public Game game;

    // from abstract PlaceConfig
    public String getManagerClassName ()
    {
        return "com.threerings.msoy.game.server.LobbyManager";
    }
}
