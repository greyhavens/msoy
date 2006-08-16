//
// $Id$

package com.threerings.msoy.game.client {

import com.threerings.io.TypedArray;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService_InvocationListener;

/**
 * Provides services for flash msoy games.
 */
public interface FlashGameService extends InvocationService
{
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
}
}
