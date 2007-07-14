//
// $Id$

package com.threerings.msoy.world.data;

import com.threerings.msoy.world.client.MsoySceneService;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.presents.client.Client;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.dobj.InvocationResponseEvent;
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
    public void moveTo (Client arg1, int arg2, int arg3, MsoyLocation arg4, SceneService.SceneMoveListener arg5)
    {
        SceneMarshaller.SceneMoveMarshaller listener5 = new SceneMarshaller.SceneMoveMarshaller();
        listener5.listener = arg5;
        sendRequest(arg1, MOVE_TO, new Object[] {
            Integer.valueOf(arg2), Integer.valueOf(arg3), arg4, listener5
        });
    }
}
