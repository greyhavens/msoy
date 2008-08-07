//
// $Id$

package com.threerings.msoy.room.server;

import com.threerings.msoy.room.client.WatcherService;

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
