//
// $Id$

package com.threerings.msoy.room.data {

import com.threerings.msoy.room.client.MsoySceneService;
import com.threerings.msoy.room.client.MsoySceneService_MsoySceneMoveListener;
import com.threerings.presents.client.Client;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.util.Integer;

/**
 * Provides the implementation of the <code>MsoySceneService</code> interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class MsoySceneMarshaller extends InvocationMarshaller
    implements MsoySceneService
{
    /** The method id used to dispatch <code>moveTo</code> requests. */
    public static const MOVE_TO :int = 1;

    // from interface MsoySceneService
    public function moveTo (arg1 :int, arg2 :int, arg3 :int, arg4 :MsoyLocation, arg5 :MsoySceneService_MsoySceneMoveListener) :void
    {
        var listener5 :MsoySceneMarshaller_MsoySceneMoveMarshaller = new MsoySceneMarshaller_MsoySceneMoveMarshaller();
        listener5.listener = arg5;
        sendRequest(MOVE_TO, [
            Integer.valueOf(arg1), Integer.valueOf(arg2), Integer.valueOf(arg3), arg4, listener5
        ]);
    }
}
}
