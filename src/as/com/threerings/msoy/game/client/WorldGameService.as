//
// $Id$

package com.threerings.msoy.game.client {

import com.threerings.io.TypedArray;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

/**
 * An ActionScript version of the Java WorldGameService interface.
 */
public interface WorldGameService extends InvocationService
{
    // from Java interface WorldGameService
    function inviteFriends (arg1 :Client, arg2 :int, arg3 :TypedArray /* of int */) :void;

    // from Java interface WorldGameService
    function locateGame (arg1 :Client, arg2 :int, arg3 :WorldGameService_LocationListener) :void;
}
}
