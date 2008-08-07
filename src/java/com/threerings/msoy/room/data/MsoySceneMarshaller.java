//
// $Id$

package com.threerings.msoy.room.data;

import com.threerings.msoy.room.client.MsoySceneService;
import com.threerings.presents.client.Client;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.whirled.client.SceneService;
import com.threerings.whirled.data.SceneMarshaller;

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
    public static final int MOVE_TO = 1;

    // from interface MsoySceneService
    public void moveTo (Client arg1, int arg2, int arg3, int arg4, MsoyLocation arg5, SceneService.SceneMoveListener arg6)
    {
        SceneMarshaller.SceneMoveMarshaller listener6 = new SceneMarshaller.SceneMoveMarshaller();
        listener6.listener = arg6;
        sendRequest(arg1, MOVE_TO, new Object[] {
            Integer.valueOf(arg2), Integer.valueOf(arg3), Integer.valueOf(arg4), arg5, listener6
        });
    }
}
