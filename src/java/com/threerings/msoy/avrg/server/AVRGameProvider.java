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
    void completeTask (ClientObject caller, String arg1, float arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link AVRGameService#setTicker} request.
     */
    void setTicker (ClientObject caller, String arg1, int arg2, InvocationService.InvocationListener arg3)
        throws InvocationException;
}
