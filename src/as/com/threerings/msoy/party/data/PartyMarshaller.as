//
// $Id$

package com.threerings.msoy.party.data {

import com.threerings.msoy.party.client.PartyService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService_ConfirmListener;
import com.threerings.presents.client.InvocationService_ResultListener;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ConfirmMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ResultMarshaller;
import com.threerings.util.Integer;

/**
 * Provides the implementation of the {@link PartyService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class PartyMarshaller extends InvocationMarshaller
    implements PartyService
{
    /** The method id used to dispatch {@link #joinParty} requests. */
    public static const JOIN_PARTY :int = 1;

    // from interface PartyService
    public function joinParty (arg1 :Client, arg2 :int, arg3 :InvocationService_ResultListener) :void
    {
        var listener3 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, JOIN_PARTY, [
            Integer.valueOf(arg2), listener3
        ]);
    }

    /** The method id used to dispatch {@link #leaveParty} requests. */
    public static const LEAVE_PARTY :int = 2;

    // from interface PartyService
    public function leaveParty (arg1 :Client, arg2 :InvocationService_ConfirmListener) :void
    {
        var listener2 :InvocationMarshaller_ConfirmMarshaller = new InvocationMarshaller_ConfirmMarshaller();
        listener2.listener = arg2;
        sendRequest(arg1, LEAVE_PARTY, [
            listener2
        ]);
    }

    /** The method id used to dispatch {@link #startParty} requests. */
    public static const START_PARTY :int = 3;

    // from interface PartyService
    public function startParty (arg1 :Client, arg2 :String, arg3 :InvocationService_ResultListener) :void
    {
        var listener3 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, START_PARTY, [
            arg2, listener3
        ]);
    }
}
}
