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
     * Handles a {@link FlashGameService#sendMessage} request.
     */
    public void sendMessage (ClientObject caller, int arg1, String arg2, byte[] arg3, InvocationService.InvocationListener arg4)
        throws InvocationException;
}
