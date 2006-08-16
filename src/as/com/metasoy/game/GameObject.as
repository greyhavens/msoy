package com.metasoy.game {

import flash.events.IEventDispatcher;

/**
 * The game object that you'll be using to manage your game.
 */
public interface GameObject
    extends IEventDispatcher
{
    /**
     * Data accessor.
     */
    function get data () :Object;

    /**
     * Set a property that will be distributed. 
     */
    function set (propName :String, value :Object, index :int = -1) :void;

    /**
     * Get a property from data.
     */
    function get (propName :String, index :int = -1) :Object;

    /**
     * Send the specified message to the chatbox for this game.
     */
    function writeToLocalChat (msg :String) :void

    /**
     * Get the player names, as an array.
     */
    function getPlayerNames () :Array;

    /**
     * Get the index into the player names array of the current player,
     * or -1 if the user is not a player.
     */
    function getMyIndex () :int

    /**
     * Get the turn holder's index, or -1 if it's nobody's turn.
     */
    function getTurnHolderIndex () :int;

    /**
     * A convenience method to just check if it's our turn.
     */
    function isMyTurn () :Boolean;

    /**
     * End the current turn. If no next player index is specified,
     * then the next player after the current one is used.
     */
    function endTurn (optionalNextPlayerIndex :int = -1) :void;

    /**
     * End the game. The specified player indexes are winners!
     */
    function endGame (winnerIndex :int, ... rest) :void;
}
}
