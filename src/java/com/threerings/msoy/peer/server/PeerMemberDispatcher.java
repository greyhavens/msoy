//
// $Id$

package com.threerings.msoy.peer.server;

import com.threerings.msoy.data.all.MemberName;
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

    @Override // documentation inherited
    public InvocationMarshaller createMarshaller ()
    {
        return new PeerMemberMarshaller();
    }

    @SuppressWarnings("unchecked")
    @Override // documentation inherited
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case PeerMemberMarshaller.DISPLAY_NAME_CHANGED:
            ((PeerMemberProvider)provider).displayNameChanged(
                source,
                (MemberName)args[0]
            );
            return;

        case PeerMemberMarshaller.FLOW_UPDATED:
            ((PeerMemberProvider)provider).flowUpdated(
                source,
                ((Integer)args[0]).intValue(), ((Integer)args[1]).intValue(), ((Integer)args[2]).intValue()
            );
            return;

        case PeerMemberMarshaller.REPORT_UNREAD_MAIL:
            ((PeerMemberProvider)provider).reportUnreadMail(
                source,
                ((Integer)args[0]).intValue(), ((Integer)args[1]).intValue()
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
