//
// $Id$

package com.threerings.msoy.world.server;

import com.threerings.msoy.world.client.WorldService;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link WorldService}.
 */
public interface WorldProvider extends InvocationProvider
{
    /**
     * Handles a {@link WorldService#getHomeId} request.
     */
    void getHomeId (ClientObject caller, byte arg1, int arg2, InvocationService.ResultListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link WorldService#getHomePageGridItems} request.
     */
    void getHomePageGridItems (ClientObject caller, InvocationService.ResultListener arg1)
        throws InvocationException;

    /**
     * Handles a {@link WorldService#setHomeSceneId} request.
     */
    void setHomeSceneId (ClientObject caller, int arg1, int arg2, int arg3, InvocationService.ConfirmListener arg4)
        throws InvocationException;
}
