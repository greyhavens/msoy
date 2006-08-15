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
     * Get the player names, as an array.
     */
    function getPlayerNames () :Array;

    /**
     * Get the index into the player names array of the current player,
     * or -1 if the user is not a player.
     */
    function getMyIndex () :int

    //function endTurn (optionalNextPlayerIndex :int = -1) :void
}
}
