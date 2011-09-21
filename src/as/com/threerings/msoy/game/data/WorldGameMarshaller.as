//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.io.TypedArray;

import com.threerings.util.Integer;

import com.threerings.presents.client.InvocationService_ResultListener;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ResultMarshaller;

import com.threerings.msoy.game.client.WorldGameService;
import com.threerings.msoy.game.client.WorldGameService_LocationListener;

/**
 * Provides the implementation of the <code>WorldGameService</code> interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class WorldGameMarshaller extends InvocationMarshaller
    implements WorldGameService
{
    /** The method id used to dispatch <code>getTablesWaiting</code> requests. */
    public static const GET_TABLES_WAITING :int = 1;

    // from interface WorldGameService
    public function getTablesWaiting (arg1 :InvocationService_ResultListener) :void
    {
        var listener1 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener1.listener = arg1;
        sendRequest(GET_TABLES_WAITING, [
            listener1
        ]);
    }

    /** The method id used to dispatch <code>inviteFriends</code> requests. */
    public static const INVITE_FRIENDS :int = 2;

    // from interface WorldGameService
    public function inviteFriends (arg1 :int, arg2 :TypedArray /* of int */) :void
    {
        sendRequest(INVITE_FRIENDS, [
            Integer.valueOf(arg1), arg2
        ]);
    }

    /** The method id used to dispatch <code>locateGame</code> requests. */
    public static const LOCATE_GAME :int = 3;

    // from interface WorldGameService
    public function locateGame (arg1 :int, arg2 :WorldGameService_LocationListener) :void
    {
        var listener2 :WorldGameMarshaller_LocationMarshaller = new WorldGameMarshaller_LocationMarshaller();
        listener2.listener = arg2;
        sendRequest(LOCATE_GAME, [
            Integer.valueOf(arg1), listener2
        ]);
    }
}
}
