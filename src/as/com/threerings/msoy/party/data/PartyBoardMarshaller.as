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
    public function createParty (arg1 :Client, arg2 :Currency, arg3 :int, arg4 :String, arg5 :int, arg6 :Boolean, arg7 :PartyBoardService_JoinListener) :void
    {
        var listener7 :PartyBoardMarshaller_JoinMarshaller = new PartyBoardMarshaller_JoinMarshaller();
        listener7.listener = arg7;
        sendRequest(arg1, CREATE_PARTY, [
            arg2, Integer.valueOf(arg3), arg4, Integer.valueOf(arg5), langBoolean.valueOf(arg6), listener7
        ]);
    }

    /** The method id used to dispatch <code>getCreateCost</code> requests. */
    public static const GET_CREATE_COST :int = 2;

    // from interface PartyBoardService
    public function getCreateCost (arg1 :Client, arg2 :InvocationService_ResultListener) :void
    {
        var listener2 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(arg1, GET_CREATE_COST, [
            listener2
        ]);
    }

    /** The method id used to dispatch <code>getPartyBoard</code> requests. */
    public static const GET_PARTY_BOARD :int = 3;

    // from interface PartyBoardService
    public function getPartyBoard (arg1 :Client, arg2 :int, arg3 :InvocationService_ResultListener) :void
    {
        var listener3 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, GET_PARTY_BOARD, [
            Byte.valueOf(arg2), listener3
        ]);
    }

    /** The method id used to dispatch <code>getPartyDetail</code> requests. */
    public static const GET_PARTY_DETAIL :int = 4;

    // from interface PartyBoardService
    public function getPartyDetail (arg1 :Client, arg2 :int, arg3 :InvocationService_ResultListener) :void
    {
        var listener3 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, GET_PARTY_DETAIL, [
            Integer.valueOf(arg2), listener3
        ]);
    }

    /** The method id used to dispatch <code>locateParty</code> requests. */
    public static const LOCATE_PARTY :int = 5;

    // from interface PartyBoardService
    public function locateParty (arg1 :Client, arg2 :int, arg3 :PartyBoardService_JoinListener) :void
    {
        var listener3 :PartyBoardMarshaller_JoinMarshaller = new PartyBoardMarshaller_JoinMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, LOCATE_PARTY, [
            Integer.valueOf(arg2), listener3
        ]);
    }
}
}
