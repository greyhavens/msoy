//
// $Id$

package com.threerings.msoy.peer.server;

import com.threerings.msoy.peer.client.PeerMemberService;
import com.threerings.msoy.peer.data.PeerMemberMarshaller;
import com.threerings.presents.client.Client;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;

/**
 * Dispatches requests to the {@link PeerMemberProvider}.
 */
public class PeerMemberDispatcher extends InvocationDispatcher
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public PeerMemberDispatcher (PeerMemberProvider provider)
    {
        this.provider = provider;
    }

    // from InvocationDispatcher
    public InvocationMarshaller createMarshaller ()
    {
        return new PeerMemberMarshaller();
    }

    @SuppressWarnings("unchecked") // from InvocationDispatcher
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case PeerMemberMarshaller.REPORT_UNREAD_MAIL:
            ((PeerMemberProvider)provider).reportUnreadMail(
                source,
                ((Integer)args[0]).intValue(), ((Boolean)args[1]).booleanValue()
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
