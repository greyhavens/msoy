//
// $Id$

package com.threerings.msoy.game.client {

import com.threerings.parlor.util.ParlorContext;

import com.threerings.msoy.client.BaseContext;

import com.threerings.msoy.game.data.PlayerObject;

/**
 * Provides access to our various game services.
 */
public interface GameContext extends ParlorContext
{
    /**
     * Returns the context we use to obtain basic client services.
     */
    function getBaseContext () :BaseContext;

    /**
     * Returns our client object casted as a PlayerObject.
     */
    function getPlayerObject () :PlayerObject;

    /**
     * Requests that we return to Whirled, optionally redisplaying the game lobby.
     */
    function backToWhirled (showLobby :Boolean) :void;
}
}
