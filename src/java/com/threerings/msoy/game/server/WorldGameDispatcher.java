//
// $Id$

package com.threerings.msoy.game.server;

import com.threerings.msoy.game.client.WorldGameService;
import com.threerings.msoy.game.data.WorldGameMarshaller;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;

/**
 * Dispatches requests to the {@link WorldGameProvider}.
 */
public class WorldGameDispatcher extends InvocationDispatcher<WorldGameMarshaller>
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public WorldGameDispatcher (WorldGameProvider provider)
    {
        this.provider = provider;
    }

    @Override // documentation inherited
    public WorldGameMarshaller createMarshaller ()
    {
        return new WorldGameMarshaller();
    }

    @Override // documentation inherited
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case WorldGameMarshaller.INVITE_FRIENDS:
            ((WorldGameProvider)provider).inviteFriends(
                source, ((Integer)args[0]).intValue(), (int[])args[1]
            );
            return;

        case WorldGameMarshaller.LOCATE_GAME:
            ((WorldGameProvider)provider).locateGame(
                source, ((Integer)args[0]).intValue(), (WorldGameService.LocationListener)args[1]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
