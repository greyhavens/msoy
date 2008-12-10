//
// $Id$

package com.threerings.msoy.party.data;

import com.threerings.msoy.data.all.VizMemberName;
import com.threerings.msoy.party.client.PeerPartyService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;

/**
 * Provides the implementation of the {@link PeerPartyService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class PeerPartyMarshaller extends InvocationMarshaller
    implements PeerPartyService
{
    /** The method id used to dispatch {@link #getPartyDetail} requests. */
    public static final int GET_PARTY_DETAIL = 1;

    // from interface PeerPartyService
    public void getPartyDetail (Client arg1, int arg2, InvocationService.ResultListener arg3)
    {
        InvocationMarshaller.ResultMarshaller listener3 = new InvocationMarshaller.ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, GET_PARTY_DETAIL, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #joinParty} requests. */
    public static final int JOIN_PARTY = 2;

    // from interface PeerPartyService
    public void joinParty (Client arg1, int arg2, VizMemberName arg3, byte arg4, InvocationService.ResultListener arg5)
    {
        InvocationMarshaller.ResultMarshaller listener5 = new InvocationMarshaller.ResultMarshaller();
        listener5.listener = arg5;
        sendRequest(arg1, JOIN_PARTY, new Object[] {
            Integer.valueOf(arg2), arg3, Byte.valueOf(arg4), listener5
        });
    }
}
