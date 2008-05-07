//
// $Id$

package com.threerings.msoy.avrg.server;

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
    public void cancelQuest (ClientObject caller, String arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link AVRGameService#completeQuest} request.
     */
    public void completeQuest (ClientObject caller, String arg1, float arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link AVRGameService#deletePlayerProperty} request.
     */
    public void deletePlayerProperty (ClientObject caller, String arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link AVRGameService#deleteProperty} request.
     */
    public void deleteProperty (ClientObject caller, String arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link AVRGameService#sendMessage} request.
     */
    public void sendMessage (ClientObject caller, String arg1, Object arg2, int arg3, InvocationService.InvocationListener arg4)
        throws InvocationException;

    /**
     * Handles a {@link AVRGameService#setPlayerProperty} request.
     */
    public void setPlayerProperty (ClientObject caller, String arg1, byte[] arg2, boolean arg3, InvocationService.ConfirmListener arg4)
        throws InvocationException;

    /**
     * Handles a {@link AVRGameService#setProperty} request.
     */
    public void setProperty (ClientObject caller, String arg1, byte[] arg2, boolean arg3, InvocationService.ConfirmListener arg4)
        throws InvocationException;

    /**
     * Handles a {@link AVRGameService#setTicker} request.
     */
    public void setTicker (ClientObject caller, String arg1, int arg2, InvocationService.InvocationListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link AVRGameService#startQuest} request.
     */
    public void startQuest (ClientObject caller, String arg1, String arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link AVRGameService#updateQuest} request.
     */
    public void updateQuest (ClientObject caller, String arg1, int arg2, String arg3, InvocationService.ConfirmListener arg4)
        throws InvocationException;
}
