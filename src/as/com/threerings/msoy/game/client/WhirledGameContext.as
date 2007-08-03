//
// $Id$

package com.threerings.msoy.game.client {

import com.threerings.crowd.util.CrowdContext;

import com.threerings.msoy.client.TopPanel;

/**
 * Provides access to the services needed by WhirledGameControlBackend.
 */
public interface WhirledGameContext extends CrowdContext
{
    /**
     * Provides access to the main UI panel.
     */
    function getTopPanel () :TopPanel;

    /**
     * Displays the specified game lobby.
     */
    function displayLobby (gameId :int) :void;
}
}
