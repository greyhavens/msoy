//
// $Id$

package com.threerings.msoy.avrg.server;

import com.threerings.msoy.avrg.client.AVRGameService;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link AVRGameService}.
 */
public interface AVRGameProvider extends InvocationProvider
{
    /**
     * Handles a {@link AVRGameService#completeTask} request.
     */
    void completeTask (ClientObject caller, int arg1, String arg2, float arg3, InvocationService.ConfirmListener arg4)
        throws InvocationException;

    /**
     * Handles a {@link AVRGameService#loadOfflinePlayer} request.
     */
    void loadOfflinePlayer (ClientObject caller, int arg1, InvocationService.ResultListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link AVRGameService#setOfflinePlayerProperty} request.
     */
    void setOfflinePlayerProperty (ClientObject caller, int arg1, String arg2, Object arg3, Integer arg4, boolean arg5, InvocationService.ConfirmListener arg6)
        throws InvocationException;
}
