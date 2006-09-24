//
// $Id$

package com.threerings.msoy.game.server;

import com.threerings.msoy.game.client.LobbyService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link LobbyService}.
 */
public interface LobbyProvider extends InvocationProvider
{
    /**
     * Handles a {@link LobbyService#identifyLobby} request.
     */
    public void identifyLobby (ClientObject caller, int arg1, InvocationService.ResultListener arg2)
        throws InvocationException;
}
