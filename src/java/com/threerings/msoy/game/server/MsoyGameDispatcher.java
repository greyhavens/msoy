//
// $Id$

package com.threerings.msoy.game.server;

import com.threerings.msoy.game.client.MsoyGameService;
import com.threerings.msoy.game.data.MsoyGameMarshaller;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;

/**
 * Dispatches requests to the {@link MsoyGameProvider}.
 */
public class MsoyGameDispatcher extends InvocationDispatcher<MsoyGameMarshaller>
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public MsoyGameDispatcher (MsoyGameProvider provider)
    {
        this.provider = provider;
    }

    @Override // documentation inherited
    public MsoyGameMarshaller createMarshaller ()
    {
        return new MsoyGameMarshaller();
    }

    @Override // documentation inherited
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case MsoyGameMarshaller.INVITE_FRIENDS:
            ((MsoyGameProvider)provider).inviteFriends(
                source, ((Integer)args[0]).intValue(), (int[])args[1]
            );
            return;

        case MsoyGameMarshaller.LOCATE_GAME:
            ((MsoyGameProvider)provider).locateGame(
                source, ((Integer)args[0]).intValue(), (MsoyGameService.LocationListener)args[1]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
