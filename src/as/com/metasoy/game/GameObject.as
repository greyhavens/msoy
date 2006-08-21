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
     * Get a property from data.
     */
    function get (propName :String, index :int = -1) :Object;

    /**
     * Set a property that will be distributed. 
     */
    function set (propName :String, value :Object, index :int = -1) :void;

    /**
     * Set the specified collection to contain the specified values,
     * clearing any previous values.
     */
    function setCollection (collName :String, values :Array) :void;

    /**
     * Add to an existing collection. If it doesn't exist, it will
     * be created. The new values will be inserted randomly into the
     * collection.
     */
    function addToCollection (collName :String, values :Array) :void;

    /**
     * Pick (do not remove) the specified number of elements from a collection,
     * and distribute them to a specific player or set them as a property
     * in the game data.
     */
    // TODO: a way to specify exclusive picks vs. duplicate-OK picks?
    function pickFromCollection (
        collName :String, count :int, msgOrPropName :String,
        playerIndex :int = -1) :void;

    /**
     * Deal (remove) the specified number of elements from a collection,
     * and distribute them to a specific player or set them as a property
     * in the game data.
     */
    // TODO: figure out the method signature of the callback
    function dealFromCollection (
        collName :String, count :int, msgOrPropName :String,
        callBack :Function = null, playerIndex :int = -1) :void;

    /**
     * Merge the specified collection into the other collection.
     * The source collection will be destroyed. The elements from
     * The source collection will be shuffled and appended to the end
     * of the destination collection.
     */
    function mergeCollection (srcColl :String, intoColl :String) :void;

    /**
     * Send a "message" to other clients subscribed to the game.
     * These is similar to setting a property, except that the
     * value will not be saved- it will merely end up coming out
     * as a MessageReceivedEvent.
     *
     * @param playerIndex if -1, sends to all players, otherwise
     * the message will be private to just one player
     */
    function sendMessage (
        messageName :String, value :Object, playerIndex :int = -1) :void;

    /**
     * Send a message that will be heard by everyone in the game room,
     * even observers.
     */
    function sendChat (msg :String) :void;

    /**
     * Display the specified message immediately locally: not sent
     * to any other players or observers in the game room.
     */
    function localChat (msg :String) :void;

    /**
     * Get the player names, as an array.
     */
    function getPlayerNames () :Array /* of String */;

    /**
     * Get the index into the player names array of the current player,
     * or -1 if the user is not a player.
     */
    function getMyIndex () :int;

    /**
     * Get the turn holder's index, or -1 if it's nobody's turn.
     */
    function getTurnHolderIndex () :int;

    /**
     * Get the indexes of the winners
     */
    function getWinnerIndexes () :Array /* of int */;

    /**
     * A convenience method to just check if it's our turn.
     */
    function isMyTurn () :Boolean;

    /**
     * Is the game currently in play?
     */
    function isInPlay () :Boolean;

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
