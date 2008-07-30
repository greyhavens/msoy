//
// $Id$

package com.threerings.msoy.game.server;

import com.threerings.msoy.game.data.LobbyMarshaller;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;

/**
 * Dispatches requests to the {@link LobbyProvider}.
 */
public class LobbyDispatcher extends InvocationDispatcher<LobbyMarshaller>
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public LobbyDispatcher (LobbyProvider provider)
    {
        this.provider = provider;
    }

    @Override // documentation inherited
    public LobbyMarshaller createMarshaller ()
    {
        return new LobbyMarshaller();
    }

    @Override // documentation inherited
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case LobbyMarshaller.IDENTIFY_LOBBY:
            ((LobbyProvider)provider).identifyLobby(
                source, ((Integer)args[0]).intValue(), (InvocationService.ResultListener)args[1]
            );
            return;

        case LobbyMarshaller.JOIN_PLAYER_GAME:
            ((LobbyProvider)provider).joinPlayerGame(
                source, ((Integer)args[0]).intValue(), (InvocationService.ResultListener)args[1]
            );
            return;

        case LobbyMarshaller.PLAY_NOW:
            ((LobbyProvider)provider).playNow(
                source, ((Integer)args[0]).intValue(), ((Integer)args[1]).intValue(), (InvocationService.ResultListener)args[2]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
