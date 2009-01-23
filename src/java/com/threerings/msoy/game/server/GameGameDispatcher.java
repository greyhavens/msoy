//
// $Id$

package com.threerings.msoy.game.server;

import com.threerings.msoy.game.data.GameGameMarshaller;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;

/**
 * Dispatches requests to the {@link GameGameProvider}.
 */
public class GameGameDispatcher extends InvocationDispatcher<GameGameMarshaller>
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public GameGameDispatcher (GameGameProvider provider)
    {
        this.provider = provider;
    }

    @Override // documentation inherited
    public GameGameMarshaller createMarshaller ()
    {
        return new GameGameMarshaller();
    }

    @Override // documentation inherited
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case GameGameMarshaller.GET_TROPHIES:
            ((GameGameProvider)provider).getTrophies(
                source, ((Integer)args[0]).intValue(), (InvocationService.ResultListener)args[1]
            );
            return;

        case GameGameMarshaller.REMOVE_DEVELOPMENT_TROPHIES:
            ((GameGameProvider)provider).removeDevelopmentTrophies(
                source, ((Integer)args[0]).intValue(), (InvocationService.ConfirmListener)args[1]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
