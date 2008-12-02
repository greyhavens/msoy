//
// $Id$

package com.threerings.msoy.party.data {

import com.threerings.msoy.party.client.PartyBoardService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService_ResultListener;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ResultMarshaller;
import com.threerings.util.Integer;

/**
 * Provides the implementation of the <code>PartyBoardService</code> interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class PartyBoardMarshaller extends InvocationMarshaller
    implements PartyBoardService
{
    /** The method id used to dispatch <code>createParty</code> requests. */
    public static const CREATE_PARTY :int = 1;

    // from interface PartyBoardService
    public function createParty (arg1 :Client, arg2 :String, arg3 :int, arg4 :InvocationService_ResultListener) :void
    {
        var listener4 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, CREATE_PARTY, [
            arg2, Integer.valueOf(arg3), listener4
        ]);
    }

    /** The method id used to dispatch <code>getPartyBoard</code> requests. */
    public static const GET_PARTY_BOARD :int = 2;

    // from interface PartyBoardService
    public function getPartyBoard (arg1 :Client, arg2 :String, arg3 :InvocationService_ResultListener) :void
    {
        var listener3 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, GET_PARTY_BOARD, [
            arg2, listener3
        ]);
    }

    /** The method id used to dispatch <code>joinParty</code> requests. */
    public static const JOIN_PARTY :int = 3;

    // from interface PartyBoardService
    public function joinParty (arg1 :Client, arg2 :int, arg3 :InvocationService_ResultListener) :void
    {
        var listener3 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, JOIN_PARTY, [
            Integer.valueOf(arg2), listener3
        ]);
    }
}
}
