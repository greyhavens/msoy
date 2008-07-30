//
// $Id$

package com.threerings.msoy.bureau.server;

import com.threerings.msoy.bureau.data.ThaneWorldMarshaller;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;

/**
 * Dispatches requests to the {@link ThaneWorldProvider}.
 */
public class ThaneWorldDispatcher extends InvocationDispatcher<ThaneWorldMarshaller>
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public ThaneWorldDispatcher (ThaneWorldProvider provider)
    {
        this.provider = provider;
    }

    @Override // documentation inherited
    public ThaneWorldMarshaller createMarshaller ()
    {
        return new ThaneWorldMarshaller();
    }

    @Override // documentation inherited
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case ThaneWorldMarshaller.LOCATE_ROOM:
            ((ThaneWorldProvider)provider).locateRoom(
                source, ((Integer)args[0]).intValue(), (InvocationService.ResultListener)args[1]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
