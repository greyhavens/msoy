//
// $Id$

package com.threerings.msoy.game.client {

import com.threerings.parlor.util.ParlorContext;

import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.data.all.MemberName;

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
     * Return this client's member name.
     */
    function getMyName () :MemberName;

    /**
     * Return this client's member id, or 0 if we're logged off or the viewer.
     */
    function getMyId () :int;

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
    function showGameLobby () :void;
    
    /**
     * Requests that we open the appropriate area of the game's shop.
     */
    function showGameShop (itemType :int, catalogId :int = 0) :void;
    
    /**
     * Requests that we open the game invite page.
     */
    function showInvitePage (defmsg :String, token :String = "", roomId :int = 0) :void;

    /**
     * Requests that we display the trophies awarded by this game.
     */
    function showTrophies () :void;

    /**
     * Returns an array of FriendEntry records for this player's online friends.
     */
    function getOnlineFriends () :Array;
    
    /**
     * Retrieves the invite token that was passed to the game.
     */
    function getInviteToken () :String;
    
    /**
     * Retrieves the ID of the member who invited the current player to this game.
     */
    function getInviterMemberId () :int;
}
}
