//
// $Id$

package com.threerings.msoy.peer.server;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.PlayerMetrics;
import com.threerings.msoy.peer.client.MsoyPeerService;
import com.threerings.msoy.peer.data.MsoyPeerMarshaller;
import com.threerings.presents.client.Client;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;
import com.threerings.stats.data.StatSet;

/**
 * Dispatches requests to the {@link MsoyPeerProvider}.
 */
public class MsoyPeerDispatcher extends InvocationDispatcher
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public MsoyPeerDispatcher (MsoyPeerProvider provider)
    {
        this.provider = provider;
    }

    @Override // documentation inherited
    public InvocationMarshaller createMarshaller ()
    {
        return new MsoyPeerMarshaller();
    }

    @SuppressWarnings("unchecked")
    @Override // documentation inherited
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case MsoyPeerMarshaller.FORWARD_MEMBER_OBJECT:
            ((MsoyPeerProvider)provider).forwardMemberObject(
                source,
                (MemberObject)args[0], (String)args[1], (StatSet)args[2], (PlayerMetrics)args[3]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
