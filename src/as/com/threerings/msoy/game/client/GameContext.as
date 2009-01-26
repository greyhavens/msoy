//
// $Id$

package com.threerings.msoy.game.client {

import com.threerings.parlor.util.ParlorContext;

import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.game.data.PlayerObject;

/**
 * Provides access to our various game services.
 */
public interface GameContext extends ParlorContext
{
    /**
     * Returns the context we use to obtain basic client services.
     */
    function getMsoyContext () :MsoyContext;

    /**
     * Returns our client object casted as a PlayerObject.
     */
    function getPlayerObject () :PlayerObject;

    /**
     * Requests that we return to Whirled, optionally redisplaying the game lobby.
     */
    function backToWhirled (showLobby :Boolean) :void;

    /**
     * Displays the active game's instructions.
     */
    function showGameInstructions () :void;

    /**
     * Requests that we display the game's lobby. 
     */
    function showGameLobby (multiplayer :Boolean) :void;
    
    /**
     * Requests that we open the appropriate area of the game's shop.
     */
    function showGameShop (itemType :int, catalogId :int = 0) :void;
    
    /**
     * Requests that we open the share game page.
     */
    function showSharePage (defmsg :String, token :String = "", roomId :int = 0) :void;

    /**
     * Requests that we display the trophies awarded by this game.
     */
    function showTrophies () :void;

    /**
     * Returns an array of FriendEntry records for this player's online friends.
     */
    function getOnlineFriends () :Array;
    
    /**
     * Retrieves the share token that was passed to the game.
     */
    function getShareToken () :String;
    
    /**
     * Retrieves the ID of the member who shared this game.
     */
    function getShareMemberId () :int;
}
}
