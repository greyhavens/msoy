//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.msoy.game.client.GameGameService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService_ConfirmListener;
import com.threerings.presents.client.InvocationService_ResultListener;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ConfirmMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ResultMarshaller;
import com.threerings.util.Integer;

/**
 * Provides the implementation of the <code>GameGameService</code> interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class GameGameMarshaller extends InvocationMarshaller
    implements GameGameService
{
    /** The method id used to dispatch <code>complainPlayer</code> requests. */
    public static const COMPLAIN_PLAYER :int = 1;

    // from interface GameGameService
    public function complainPlayer (arg1 :int, arg2 :String) :void
    {
        sendRequest(COMPLAIN_PLAYER, [
            Integer.valueOf(arg1), arg2
        ]);
    }

    /** The method id used to dispatch <code>getTrophies</code> requests. */
    public static const GET_TROPHIES :int = 2;

    // from interface GameGameService
    public function getTrophies (arg1 :int, arg2 :InvocationService_ResultListener) :void
    {
        var listener2 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(GET_TROPHIES, [
            Integer.valueOf(arg1), listener2
        ]);
    }

    /** The method id used to dispatch <code>removeDevelopmentTrophies</code> requests. */
    public static const REMOVE_DEVELOPMENT_TROPHIES :int = 3;

    // from interface GameGameService
    public function removeDevelopmentTrophies (arg1 :int, arg2 :InvocationService_ConfirmListener) :void
    {
        var listener2 :InvocationMarshaller_ConfirmMarshaller = new InvocationMarshaller_ConfirmMarshaller();
        listener2.listener = arg2;
        sendRequest(REMOVE_DEVELOPMENT_TROPHIES, [
            Integer.valueOf(arg1), listener2
        ]);
    }
}
}
