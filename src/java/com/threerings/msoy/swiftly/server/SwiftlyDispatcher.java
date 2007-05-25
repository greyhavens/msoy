//
// $Id$

package com.threerings.msoy.swiftly.server;

import com.threerings.msoy.swiftly.data.SwiftlyMarshaller;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;

/**
 * Dispatches requests to the {@link SwiftlyProvider}.
 */
public class SwiftlyDispatcher extends InvocationDispatcher
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public SwiftlyDispatcher (SwiftlyProvider provider)
    {
        this.provider = provider;
    }

    // from InvocationDispatcher
    @Override
    public InvocationMarshaller createMarshaller ()
    {
        return new SwiftlyMarshaller();
    }

    @Override
    @SuppressWarnings("unchecked") // from InvocationDispatcher
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case SwiftlyMarshaller.ENTER_PROJECT:
            ((SwiftlyProvider)provider).enterProject(
                source,
                ((Integer)args[0]).intValue(), (InvocationService.ResultListener)args[1]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
