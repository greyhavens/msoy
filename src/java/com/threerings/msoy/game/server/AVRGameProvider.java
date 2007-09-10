//
// $Id$

package com.threerings.msoy.game.server;

import com.threerings.msoy.game.client.AVRGameService;
import com.threerings.msoy.world.data.MemoryEntry;
import com.threerings.presents.client.Client;
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
     * Handles a {@link AVRGameService#joinAVRGame} request.
     */
    public void joinAVRGame (ClientObject caller, int arg1, InvocationService.InvocationListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link AVRGameService#leaveAVRGame} request.
     */
    public void leaveAVRGame (ClientObject caller, InvocationService.InvocationListener arg1)
        throws InvocationException;

    /**
     * Handles a {@link AVRGameService#updateMemory} request.
     */
    public void updateMemory (ClientObject caller, MemoryEntry arg1);
}
