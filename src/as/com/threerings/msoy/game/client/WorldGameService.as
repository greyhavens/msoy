//
// $Id$

package com.threerings.msoy.game.client {

import com.threerings.io.TypedArray;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService_ResultListener;

/**
 * An ActionScript version of the Java WorldGameService interface.
 */
public interface WorldGameService extends InvocationService
{
    // from Java interface WorldGameService
    function getTablesWaiting (arg1 :InvocationService_ResultListener) :void;

    // from Java interface WorldGameService
    function inviteFriends (arg1 :int, arg2 :TypedArray /* of int */) :void;

    // from Java interface WorldGameService
    function locateGame (arg1 :int, arg2 :WorldGameService_LocationListener) :void;
}
}
