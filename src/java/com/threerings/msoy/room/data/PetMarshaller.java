//
// $Id$

package com.threerings.msoy.room.data;

import com.threerings.msoy.room.client.PetService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;

/**
 * Provides the implementation of the {@link PetService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class PetMarshaller extends InvocationMarshaller
    implements PetService
{
    /** The method id used to dispatch {@link #callPet} requests. */
    public static final int CALL_PET = 1;

    // from interface PetService
    public void callPet (Client arg1, int arg2, InvocationService.ConfirmListener arg3)
    {
        InvocationMarshaller.ConfirmMarshaller listener3 = new InvocationMarshaller.ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, CALL_PET, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #orderPet} requests. */
    public static final int ORDER_PET = 2;

    // from interface PetService
    public void orderPet (Client arg1, int arg2, int arg3, InvocationService.ConfirmListener arg4)
    {
        InvocationMarshaller.ConfirmMarshaller listener4 = new InvocationMarshaller.ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, ORDER_PET, new Object[] {
            Integer.valueOf(arg2), Integer.valueOf(arg3), listener4
        });
    }

    /** The method id used to dispatch {@link #sendChat} requests. */
    public static final int SEND_CHAT = 3;

    // from interface PetService
    public void sendChat (Client arg1, int arg2, int arg3, String arg4, InvocationService.ConfirmListener arg5)
    {
        InvocationMarshaller.ConfirmMarshaller listener5 = new InvocationMarshaller.ConfirmMarshaller();
        listener5.listener = arg5;
        sendRequest(arg1, SEND_CHAT, new Object[] {
            Integer.valueOf(arg2), Integer.valueOf(arg3), arg4, listener5
        });
    }
}
