//
// $Id$

package com.threerings.msoy.game.server;

import javax.annotation.Generated;

import com.threerings.msoy.game.client.WorldGameService;
import com.threerings.msoy.game.data.WorldGameMarshaller;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;

/**
 * Dispatches requests to the {@link WorldGameProvider}.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from WorldGameService.java.")
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

    @Override
    public WorldGameMarshaller createMarshaller ()
    {
        return new WorldGameMarshaller();
    }

    @Override
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case WorldGameMarshaller.GET_TABLES_WAITING:
            ((WorldGameProvider)provider).getTablesWaiting(
                source, (InvocationService.ResultListener)args[0]
            );
            return;

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
