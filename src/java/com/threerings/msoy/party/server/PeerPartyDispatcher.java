//
// $Id$

package com.threerings.msoy.party.server;

import com.threerings.msoy.data.all.VizMemberName;
import com.threerings.msoy.party.data.PeerPartyMarshaller;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;

/**
 * Dispatches requests to the {@link PeerPartyProvider}.
 */
public class PeerPartyDispatcher extends InvocationDispatcher<PeerPartyMarshaller>
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public PeerPartyDispatcher (PeerPartyProvider provider)
    {
        this.provider = provider;
    }

    @Override // documentation inherited
    public PeerPartyMarshaller createMarshaller ()
    {
        return new PeerPartyMarshaller();
    }

    @Override // documentation inherited
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case PeerPartyMarshaller.GET_PARTY_DETAIL:
            ((PeerPartyProvider)provider).getPartyDetail(
                source, ((Integer)args[0]).intValue(), (InvocationService.ResultListener)args[1]
            );
            return;

        case PeerPartyMarshaller.GET_PARTY_SCENE:
            ((PeerPartyProvider)provider).getPartyScene(
                source, ((Integer)args[0]).intValue(), (InvocationService.ResultListener)args[1]
            );
            return;

        case PeerPartyMarshaller.JOIN_PARTY:
            ((PeerPartyProvider)provider).joinParty(
                source, ((Integer)args[0]).intValue(), (VizMemberName)args[1], ((Byte)args[2]).byteValue(), (InvocationService.ResultListener)args[3]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
