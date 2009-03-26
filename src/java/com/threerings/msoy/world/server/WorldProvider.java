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
     * Handles a {@link WorldService#getGroupHomeSceneId} request.
     */
    void getGroupHomeSceneId (ClientObject caller, int arg1, InvocationService.ResultListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link WorldService#getHomePageGridItems} request.
     */
    void getHomePageGridItems (ClientObject caller, InvocationService.ResultListener arg1)
        throws InvocationException;
}
