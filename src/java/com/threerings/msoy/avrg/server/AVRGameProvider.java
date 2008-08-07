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
     * Handles a {@link AVRGameService#cancelQuest} request.
     */
    void cancelQuest (ClientObject caller, String arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link AVRGameService#completeQuest} request.
     */
    void completeQuest (ClientObject caller, String arg1, float arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link AVRGameService#sendMessage} request.
     */
    void sendMessage (ClientObject caller, String arg1, Object arg2, int arg3, InvocationService.InvocationListener arg4)
        throws InvocationException;

    /**
     * Handles a {@link AVRGameService#setTicker} request.
     */
    void setTicker (ClientObject caller, String arg1, int arg2, InvocationService.InvocationListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link AVRGameService#startQuest} request.
     */
    void startQuest (ClientObject caller, String arg1, String arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link AVRGameService#updateQuest} request.
     */
    void updateQuest (ClientObject caller, String arg1, int arg2, String arg3, InvocationService.ConfirmListener arg4)
        throws InvocationException;
}
