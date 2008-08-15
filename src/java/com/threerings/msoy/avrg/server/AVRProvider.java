//
// $Id$

package com.threerings.msoy.avrg.server;

import com.threerings.msoy.avrg.client.AVRService;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link AVRService}.
 */
public interface AVRProvider extends InvocationProvider
{
    /**
     * Handles a {@link AVRService#activateGame} request.
     */
    void activateGame (ClientObject caller, int arg1, AVRService.AVRGameJoinListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link AVRService#deactivateGame} request.
     */
    void deactivateGame (ClientObject caller, int arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;
}
