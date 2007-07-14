//
// $Id$

package com.threerings.msoy.world.server;

import com.threerings.msoy.world.client.MsoySceneService;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.presents.client.Client;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;
import com.threerings.whirled.client.SceneService;
import com.threerings.whirled.data.SceneMarshaller;

/**
 * Defines the server-side of the {@link MsoySceneService}.
 */
public interface MsoySceneProvider extends InvocationProvider
{
    /**
     * Handles a {@link MsoySceneService#moveTo} request.
     */
    public void moveTo (ClientObject caller, int arg1, int arg2, MsoyLocation arg3, SceneService.SceneMoveListener arg4)
        throws InvocationException;
}
