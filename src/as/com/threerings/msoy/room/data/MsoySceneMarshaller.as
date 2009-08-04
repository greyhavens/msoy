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
    public function moveTo (arg1 :Client, arg2 :int, arg3 :int, arg4 :int, arg5 :MsoyLocation, arg6 :MsoySceneService_MsoySceneMoveListener) :void
    {
        var listener6 :MsoySceneMarshaller_MsoySceneMoveMarshaller = new MsoySceneMarshaller_MsoySceneMoveMarshaller();
        listener6.listener = arg6;
        sendRequest(arg1, MOVE_TO, [
            Integer.valueOf(arg2), Integer.valueOf(arg3), Integer.valueOf(arg4), arg5, listener6
        ]);
    }
}
}
