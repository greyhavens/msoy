//
// $Id$

package com.threerings.msoy.party.data;

import javax.annotation.Generated;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;

import com.threerings.msoy.party.client.PeerPartyService;

/**
 * Provides the implementation of the {@link PeerPartyService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from PeerPartyService.java.")
public class PeerPartyMarshaller extends InvocationMarshaller<ClientObject>
    implements PeerPartyService
{
    /** The method id used to dispatch {@link #getPartyDetail} requests. */
    public static final int GET_PARTY_DETAIL = 1;

    // from interface PeerPartyService
    public void getPartyDetail (int arg1, InvocationService.ResultListener arg2)
    {
        InvocationMarshaller.ResultMarshaller listener2 = new InvocationMarshaller.ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(GET_PARTY_DETAIL, new Object[] {
            Integer.valueOf(arg1), listener2
        });
    }
}
