//
// $Id$

package com.threerings.msoy.world.server;

import com.threerings.msoy.world.data.WatcherMarshaller;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;

/**
 * Dispatches requests to the {@link WatcherProvider}.
 */
public class WatcherDispatcher extends InvocationDispatcher<WatcherMarshaller>
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public WatcherDispatcher (WatcherProvider provider)
    {
        this.provider = provider;
    }

    @Override // documentation inherited
    public WatcherMarshaller createMarshaller ()
    {
        return new WatcherMarshaller();
    }

    @Override // documentation inherited
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case WatcherMarshaller.ADD_WATCH:
            ((WatcherProvider)provider).addWatch(
                source, ((Integer)args[0]).intValue()
            );
            return;

        case WatcherMarshaller.CLEAR_WATCH:
            ((WatcherProvider)provider).clearWatch(
                source, ((Integer)args[0]).intValue()
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
