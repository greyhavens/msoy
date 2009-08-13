//
// $Id$

package com.threerings.msoy.room.data {

import com.threerings.msoy.room.client.PetService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService_ConfirmListener;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ConfirmMarshaller;
import com.threerings.util.Integer;

/**
 * Provides the implementation of the <code>PetService</code> interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class PetMarshaller extends InvocationMarshaller
    implements PetService
{
    /** The method id used to dispatch <code>callPet</code> requests. */
    public static const CALL_PET :int = 1;

    // from interface PetService
    public function callPet (arg1 :int, arg2 :InvocationService_ConfirmListener) :void
    {
        var listener2 :InvocationMarshaller_ConfirmMarshaller = new InvocationMarshaller_ConfirmMarshaller();
        listener2.listener = arg2;
        sendRequest(CALL_PET, [
            Integer.valueOf(arg1), listener2
        ]);
    }

    /** The method id used to dispatch <code>orderPet</code> requests. */
    public static const ORDER_PET :int = 2;

    // from interface PetService
    public function orderPet (arg1 :int, arg2 :int, arg3 :InvocationService_ConfirmListener) :void
    {
        var listener3 :InvocationMarshaller_ConfirmMarshaller = new InvocationMarshaller_ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(ORDER_PET, [
            Integer.valueOf(arg1), Integer.valueOf(arg2), listener3
        ]);
    }

    /** The method id used to dispatch <code>sendChat</code> requests. */
    public static const SEND_CHAT :int = 3;

    // from interface PetService
    public function sendChat (arg1 :int, arg2 :int, arg3 :String, arg4 :InvocationService_ConfirmListener) :void
    {
        var listener4 :InvocationMarshaller_ConfirmMarshaller = new InvocationMarshaller_ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(SEND_CHAT, [
            Integer.valueOf(arg1), Integer.valueOf(arg2), arg3, listener4
        ]);
    }
}
}
