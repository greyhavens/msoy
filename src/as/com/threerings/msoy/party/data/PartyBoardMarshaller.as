//
// $Id$

package com.threerings.msoy.party.data {

import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.party.client.PartyBoardService;
import com.threerings.msoy.party.client.PartyBoardService_JoinListener;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService_ResultListener;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ResultMarshaller;
import com.threerings.util.Byte;
import com.threerings.util.Integer;
import com.threerings.util.langBoolean;

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
    public function createParty (arg1 :Currency, arg2 :int, arg3 :String, arg4 :int, arg5 :Boolean, arg6 :PartyBoardService_JoinListener) :void
    {
        var listener6 :PartyBoardMarshaller_JoinMarshaller = new PartyBoardMarshaller_JoinMarshaller();
        listener6.listener = arg6;
        sendRequest(CREATE_PARTY, [
            arg1, Integer.valueOf(arg2), arg3, Integer.valueOf(arg4), langBoolean.valueOf(arg5), listener6
        ]);
    }

    /** The method id used to dispatch <code>getCreateCost</code> requests. */
    public static const GET_CREATE_COST :int = 2;

    // from interface PartyBoardService
    public function getCreateCost (arg1 :InvocationService_ResultListener) :void
    {
        var listener1 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener1.listener = arg1;
        sendRequest(GET_CREATE_COST, [
            listener1
        ]);
    }

    /** The method id used to dispatch <code>getPartyBoard</code> requests. */
    public static const GET_PARTY_BOARD :int = 3;

    // from interface PartyBoardService
    public function getPartyBoard (arg1 :int, arg2 :InvocationService_ResultListener) :void
    {
        var listener2 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(GET_PARTY_BOARD, [
            Byte.valueOf(arg1), listener2
        ]);
    }

    /** The method id used to dispatch <code>getPartyDetail</code> requests. */
    public static const GET_PARTY_DETAIL :int = 4;

    // from interface PartyBoardService
    public function getPartyDetail (arg1 :int, arg2 :InvocationService_ResultListener) :void
    {
        var listener2 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(GET_PARTY_DETAIL, [
            Integer.valueOf(arg1), listener2
        ]);
    }

    /** The method id used to dispatch <code>locateParty</code> requests. */
    public static const LOCATE_PARTY :int = 5;

    // from interface PartyBoardService
    public function locateParty (arg1 :int, arg2 :PartyBoardService_JoinListener) :void
    {
        var listener2 :PartyBoardMarshaller_JoinMarshaller = new PartyBoardMarshaller_JoinMarshaller();
        listener2.listener = arg2;
        sendRequest(LOCATE_PARTY, [
            Integer.valueOf(arg1), listener2
        ]);
    }
}
}
