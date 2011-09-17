//
// $Id$

package com.threerings.msoy.game.server;

import javax.annotation.Generated;

import com.threerings.msoy.game.client.LobbyService;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link LobbyService}.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from LobbyService.java.")
public interface LobbyProvider extends InvocationProvider
{
    /**
     * Handles a {@link LobbyService#identifyLobby} request.
     */
    void identifyLobby (ClientObject caller, int arg1, InvocationService.ResultListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link LobbyService#playNow} request.
     */
    void playNow (ClientObject caller, int arg1, int arg2, InvocationService.ResultListener arg3)
        throws InvocationException;
}
