//
// $Id$

package com.threerings.msoy.avrg.data {

import com.threerings.msoy.avrg.client.AVRService;
import com.threerings.msoy.avrg.client.AVRService_AVRGameJoinListener;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService_ConfirmListener;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ConfirmMarshaller;
import com.threerings.util.Integer;

/**
 * Provides the implementation of the <code>AVRService</code> interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class AVRMarshaller extends InvocationMarshaller
    implements AVRService
{
    /** The method id used to dispatch <code>activateGame</code> requests. */
    public static const ACTIVATE_GAME :int = 1;

    // from interface AVRService
    public function activateGame (arg1 :int, arg2 :AVRService_AVRGameJoinListener) :void
    {
        var listener2 :AVRMarshaller_AVRGameJoinMarshaller = new AVRMarshaller_AVRGameJoinMarshaller();
        listener2.listener = arg2;
        sendRequest(ACTIVATE_GAME, [
            Integer.valueOf(arg1), listener2
        ]);
    }

    /** The method id used to dispatch <code>deactivateGame</code> requests. */
    public static const DEACTIVATE_GAME :int = 2;

    // from interface AVRService
    public function deactivateGame (arg1 :int, arg2 :InvocationService_ConfirmListener) :void
    {
        var listener2 :InvocationMarshaller_ConfirmMarshaller = new InvocationMarshaller_ConfirmMarshaller();
        listener2.listener = arg2;
        sendRequest(DEACTIVATE_GAME, [
            Integer.valueOf(arg1), listener2
        ]);
    }
}
}
