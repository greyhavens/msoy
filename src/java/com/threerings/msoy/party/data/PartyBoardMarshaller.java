//
// $Id$

package com.threerings.msoy.party.data;

import com.threerings.msoy.party.client.PartyBoardService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.dobj.InvocationResponseEvent;

/**
 * Provides the implementation of the {@link PartyBoardService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class PartyBoardMarshaller extends InvocationMarshaller
    implements PartyBoardService
{
    /**
     * Marshalls results to implementations of {@link PartyBoardService.JoinListener}.
     */
    public static class JoinMarshaller extends ListenerMarshaller
        implements JoinListener
    {
        /** The method id used to dispatch {@link #foundParty}
         * responses. */
        public static final int FOUND_PARTY = 1;

        // from interface JoinMarshaller
        public void foundParty (int arg1, String arg2, int arg3)
        {
            _invId = null;
            omgr.postEvent(new InvocationResponseEvent(
                               callerOid, requestId, FOUND_PARTY,
                               new Object[] { Integer.valueOf(arg1), arg2, Integer.valueOf(arg3) }, transport));
        }

        @Override // from InvocationMarshaller
        public void dispatchResponse (int methodId, Object[] args)
        {
            switch (methodId) {
            case FOUND_PARTY:
                ((JoinListener)listener).foundParty(
                    ((Integer)args[0]).intValue(), (String)args[1], ((Integer)args[2]).intValue());
                return;

            default:
                super.dispatchResponse(methodId, args);
                return;
            }
        }
    }

    /** The method id used to dispatch {@link #createParty} requests. */
    public static final int CREATE_PARTY = 1;

    // from interface PartyBoardService
    public void createParty (Client arg1, String arg2, int arg3, boolean arg4, PartyBoardService.JoinListener arg5)
    {
        PartyBoardMarshaller.JoinMarshaller listener5 = new PartyBoardMarshaller.JoinMarshaller();
        listener5.listener = arg5;
        sendRequest(arg1, CREATE_PARTY, new Object[] {
            arg2, Integer.valueOf(arg3), Boolean.valueOf(arg4), listener5
        });
    }

    /** The method id used to dispatch {@link #getPartyBoard} requests. */
    public static final int GET_PARTY_BOARD = 2;

    // from interface PartyBoardService
    public void getPartyBoard (Client arg1, String arg2, InvocationService.ResultListener arg3)
    {
        InvocationMarshaller.ResultMarshaller listener3 = new InvocationMarshaller.ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, GET_PARTY_BOARD, new Object[] {
            arg2, listener3
        });
    }

    /** The method id used to dispatch {@link #getPartyDetail} requests. */
    public static final int GET_PARTY_DETAIL = 3;

    // from interface PartyBoardService
    public void getPartyDetail (Client arg1, int arg2, InvocationService.ResultListener arg3)
    {
        InvocationMarshaller.ResultMarshaller listener3 = new InvocationMarshaller.ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, GET_PARTY_DETAIL, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #locateParty} requests. */
    public static final int LOCATE_PARTY = 4;

    // from interface PartyBoardService
    public void locateParty (Client arg1, int arg2, PartyBoardService.JoinListener arg3)
    {
        PartyBoardMarshaller.JoinMarshaller listener3 = new PartyBoardMarshaller.JoinMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, LOCATE_PARTY, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }
}
