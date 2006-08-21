//
// $Id$

package com.threerings.msoy.game.client {

import flash.utils.ByteArray;

import com.threerings.io.TypedArray;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService_ConfirmListener;
import com.threerings.presents.client.InvocationService_InvocationListener;

/**
 * Provides services for flash msoy games.
 */
public interface FlashGameService extends InvocationService
{
    /**
     * Request to set the specified property.
     */
    function setProperty (
        client :Client, propName :String, value :Object, index :int,
        listener :InvocationService_InvocationListener) :void;

    /**
     * Request to end the turn, possibly futzing the next turn holder unless
     * -1 is specified for the nextPlayerIndex.
     */
    function endTurn (
        client :Client, nextPlayerIndex :int,
        listener :InvocationService_InvocationListener) :void;

    /**
     * Request to end the game, with the specified player indices assigned
     * as winners.
     */
    function endGame (
        client :Client, winners :TypedArray /* of int */,
        listener :InvocationService_InvocationListener) :void;

    /**
     * Request to send a private message to one other player in
     * the game.
     */
    function sendMessage (
        client :Client, msgName :String, value :Object, playerIdx :int,
        listener :InvocationService_InvocationListener) :void;
 
    /**
     * Add to the specified named collection.
     *
     * @param clearExisting if true, wipe the old contents.
     */
    function addToCollection (
        client :Client, collName :String, data :TypedArray /* of ByteArray */,
        clearExisting :Boolean, listener :InvocationService_InvocationListener)
        :void;

    /**
     * Merge the specified collection into the other.
     */
    function mergeCollection (
        client :Client, srcColl :String, intoColl :String,
        listener :InvocationService_InvocationListener) :void;

    /**
     * Pick or deal some number of elements from the specified collection,
     * and either set a property in the flash object, or delivery the
     * picks to the specified player index via a game message.
     */
    function getFromCollection (
        client :Client, collName :String, consume :Boolean, count :int,
        msgOrPropName :String, playerIndex :int,
        listener :InvocationService_ConfirmListener) :void;
}
}
