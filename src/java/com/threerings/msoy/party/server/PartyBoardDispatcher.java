//
// $Id$

package com.threerings.msoy.party.server;

import com.threerings.msoy.party.data.PartyBoardMarshaller;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;

/**
 * Dispatches requests to the {@link PartyBoardProvider}.
 */
public class PartyBoardDispatcher extends InvocationDispatcher<PartyBoardMarshaller>
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public PartyBoardDispatcher (PartyBoardProvider provider)
    {
        this.provider = provider;
    }

    @Override // documentation inherited
    public PartyBoardMarshaller createMarshaller ()
    {
        return new PartyBoardMarshaller();
    }

    @Override // documentation inherited
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case PartyBoardMarshaller.CREATE_PARTY:
            ((PartyBoardProvider)provider).createParty(
                source, (String)args[0], ((Integer)args[1]).intValue(), ((Boolean)args[2]).booleanValue(), (InvocationService.ResultListener)args[3]
            );
            return;

        case PartyBoardMarshaller.GET_PARTY_BOARD:
            ((PartyBoardProvider)provider).getPartyBoard(
                source, (String)args[0], (InvocationService.ResultListener)args[1]
            );
            return;

        case PartyBoardMarshaller.GET_PARTY_DETAIL:
            ((PartyBoardProvider)provider).getPartyDetail(
                source, ((Integer)args[0]).intValue(), (InvocationService.ResultListener)args[1]
            );
            return;

        case PartyBoardMarshaller.JOIN_PARTY:
            ((PartyBoardProvider)provider).joinParty(
                source, ((Integer)args[0]).intValue(), (InvocationService.ResultListener)args[1]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
