//
// $Id$

package com.threerings.msoy.game.server;

import com.threerings.msoy.game.client.FlashGameService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link FlashGameService}.
 */
public interface FlashGameProvider extends InvocationProvider
{
    /**
     * Handles a {@link FlashGameService#addToCollection} request.
     */
    public void addToCollection (ClientObject caller, String arg1, byte[][] arg2, boolean arg3, InvocationService.InvocationListener arg4)
        throws InvocationException;

    /**
     * Handles a {@link FlashGameService#endGame} request.
     */
    public void endGame (ClientObject caller, int[] arg1, InvocationService.InvocationListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link FlashGameService#endTurn} request.
     */
    public void endTurn (ClientObject caller, int arg1, InvocationService.InvocationListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link FlashGameService#getFromCollection} request.
     */
    public void getFromCollection (ClientObject caller, String arg1, boolean arg2, int arg3, String arg4, int arg5, InvocationService.ConfirmListener arg6)
        throws InvocationException;

    /**
     * Handles a {@link FlashGameService#mergeCollection} request.
     */
    public void mergeCollection (ClientObject caller, String arg1, String arg2, InvocationService.InvocationListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link FlashGameService#sendMessage} request.
     */
    public void sendMessage (ClientObject caller, String arg1, Object arg2, int arg3, InvocationService.InvocationListener arg4)
        throws InvocationException;

    /**
     * Handles a {@link FlashGameService#setProperty} request.
     */
    public void setProperty (ClientObject caller, String arg1, Object arg2, int arg3, InvocationService.InvocationListener arg4)
        throws InvocationException;
}
