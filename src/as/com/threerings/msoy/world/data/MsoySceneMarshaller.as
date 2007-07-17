//
// $Id$

package com.threerings.msoy.world.data {

import flash.utils.ByteArray;
import com.threerings.util.*; // for Float, Integer, etc.
import com.threerings.io.TypedArray;

import com.threerings.msoy.world.client.MsoySceneService;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.presents.client.Client;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ListenerMarshaller;
import com.threerings.whirled.client.SceneService_SceneMoveListener;
import com.threerings.whirled.data.SceneMarshaller_SceneMoveMarshaller;

/**
 * Provides the implementation of the {@link MsoySceneService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class MsoySceneMarshaller extends InvocationMarshaller
    implements MsoySceneService
{
    /** The method id used to dispatch {@link #moveTo} requests. */
    public static const MOVE_TO :int = 1;

    // from interface MsoySceneService
    public function moveTo (arg1 :Client, arg2 :int, arg3 :int, arg4 :int, arg5 :MsoyLocation, arg6 :SceneService_SceneMoveListener) :void
    {
        var listener6 :SceneMarshaller_SceneMoveMarshaller = new SceneMarshaller_SceneMoveMarshaller();
        listener6.listener = arg6;
        sendRequest(arg1, MOVE_TO, [
            Integer.valueOf(arg2), Integer.valueOf(arg3), Integer.valueOf(arg4), arg5, listener6
        ]);
    }
}
}
