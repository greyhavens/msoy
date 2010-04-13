//
// $Id$

package com.threerings.msoy.room.server;

import javax.annotation.Generated;

import com.threerings.msoy.room.client.MsoySceneService;
import com.threerings.msoy.room.data.MsoyLocation;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link MsoySceneService}.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from MsoySceneService.java.")
public interface MsoySceneProvider extends InvocationProvider
{
    /**
     * Handles a {@link MsoySceneService#moveTo} request.
     */
    void moveTo (ClientObject caller, int arg1, int arg2, int arg3, MsoyLocation arg4, MsoySceneService.MsoySceneMoveListener arg5)
        throws InvocationException;
}
