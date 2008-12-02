//
// $Id$

package com.threerings.msoy.party.data;

import com.threerings.msoy.party.client.PartyBoardService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;

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
    /** The method id used to dispatch {@link #createParty} requests. */
    public static final int CREATE_PARTY = 1;

    // from interface PartyBoardService
    public void createParty (Client arg1, String arg2, int arg3, InvocationService.ResultListener arg4)
    {
        InvocationMarshaller.ResultMarshaller listener4 = new InvocationMarshaller.ResultMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, CREATE_PARTY, new Object[] {
            arg2, Integer.valueOf(arg3), listener4
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

    /** The method id used to dispatch {@link #joinParty} requests. */
    public static final int JOIN_PARTY = 3;

    // from interface PartyBoardService
    public void joinParty (Client arg1, int arg2, InvocationService.ResultListener arg3)
    {
        InvocationMarshaller.ResultMarshaller listener3 = new InvocationMarshaller.ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, JOIN_PARTY, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }
}
