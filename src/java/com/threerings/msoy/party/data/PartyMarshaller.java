//
// $Id$

package com.threerings.msoy.party.data;

import com.threerings.msoy.party.client.PartyService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;

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
    /** The method id used to dispatch {@link #bootMember} requests. */
    public static final int BOOT_MEMBER = 1;

    // from interface PartyService
    public void bootMember (Client arg1, InvocationService.ConfirmListener arg2)
    {
        InvocationMarshaller.ConfirmMarshaller listener2 = new InvocationMarshaller.ConfirmMarshaller();
        listener2.listener = arg2;
        sendRequest(arg1, BOOT_MEMBER, new Object[] {
            listener2
        });
    }

    /** The method id used to dispatch {@link #leaveParty} requests. */
    public static final int LEAVE_PARTY = 2;

    // from interface PartyService
    public void leaveParty (Client arg1, InvocationService.ConfirmListener arg2)
    {
        InvocationMarshaller.ConfirmMarshaller listener2 = new InvocationMarshaller.ConfirmMarshaller();
        listener2.listener = arg2;
        sendRequest(arg1, LEAVE_PARTY, new Object[] {
            listener2
        });
    }
}
