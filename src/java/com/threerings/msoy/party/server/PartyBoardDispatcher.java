//
// $Id$

package com.threerings.msoy.party.server;

import javax.annotation.Generated;

import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.party.client.PartyBoardService;
import com.threerings.msoy.party.data.PartyBoardMarshaller;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;

/**
 * Dispatches requests to the {@link PartyBoardProvider}.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from PartyBoardService.java.")
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

    @Override
    public PartyBoardMarshaller createMarshaller ()
    {
        return new PartyBoardMarshaller();
    }

    @Override
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case PartyBoardMarshaller.CREATE_PARTY:
            ((PartyBoardProvider)provider).createParty(
                source, (Currency)args[0], ((Integer)args[1]).intValue(), (String)args[2], ((Integer)args[3]).intValue(), ((Boolean)args[4]).booleanValue(), (PartyBoardService.JoinListener)args[5]
            );
            return;

        case PartyBoardMarshaller.GET_CREATE_COST:
            ((PartyBoardProvider)provider).getCreateCost(
                source, (InvocationService.ResultListener)args[0]
            );
            return;

        case PartyBoardMarshaller.GET_PARTY_BOARD:
            ((PartyBoardProvider)provider).getPartyBoard(
                source, ((Byte)args[0]).byteValue(), (InvocationService.ResultListener)args[1]
            );
            return;

        case PartyBoardMarshaller.GET_PARTY_DETAIL:
            ((PartyBoardProvider)provider).getPartyDetail(
                source, ((Integer)args[0]).intValue(), (InvocationService.ResultListener)args[1]
            );
            return;

        case PartyBoardMarshaller.LOCATE_PARTY:
            ((PartyBoardProvider)provider).locateParty(
                source, ((Integer)args[0]).intValue(), (PartyBoardService.JoinListener)args[1]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
