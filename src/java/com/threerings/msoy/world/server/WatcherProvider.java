//
// $Id$

package com.threerings.msoy.world.server;

import com.threerings.msoy.world.client.WatcherService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link WatcherService}.
 */
public interface WatcherProvider extends InvocationProvider
{
    /**
     * Handles a {@link WatcherService#addWatch} request.
     */
    void addWatch (ClientObject caller, int arg1);

    /**
     * Handles a {@link WatcherService#clearWatch} request.
     */
    void clearWatch (ClientObject caller, int arg1);
}
