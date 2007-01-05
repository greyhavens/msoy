//
// $Id$

package com.threerings.msoy.world.server;

import com.threerings.msoy.world.client.RoomService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;
import com.threerings.whirled.data.SceneUpdate;

/**
 * Defines the server-side of the {@link RoomService}.
 */
public interface RoomProvider extends InvocationProvider
{
    /**
     * Handles a {@link RoomService#editRoom} request.
     */
    public void editRoom (ClientObject caller, InvocationService.ResultListener arg1)
        throws InvocationException;

    /**
     * Handles a {@link RoomService#updateRoom} request.
     */
    public void updateRoom (ClientObject caller, SceneUpdate[] arg1, InvocationService.InvocationListener arg2)
        throws InvocationException;
}
