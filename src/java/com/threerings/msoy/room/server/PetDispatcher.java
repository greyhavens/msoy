//
// $Id$

package com.threerings.msoy.room.server;

import javax.annotation.Generated;

import com.threerings.msoy.room.data.PetMarshaller;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;

/**
 * Dispatches requests to the {@link PetProvider}.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from PetService.java.")
public class PetDispatcher extends InvocationDispatcher<PetMarshaller>
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public PetDispatcher (PetProvider provider)
    {
        this.provider = provider;
    }

    @Override
    public PetMarshaller createMarshaller ()
    {
        return new PetMarshaller();
    }

    @Override
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case PetMarshaller.CALL_PET:
            ((PetProvider)provider).callPet(
                source, ((Integer)args[0]).intValue(), (InvocationService.ConfirmListener)args[1]
            );
            return;

        case PetMarshaller.ORDER_PET:
            ((PetProvider)provider).orderPet(
                source, ((Integer)args[0]).intValue(), ((Integer)args[1]).intValue(), (InvocationService.ConfirmListener)args[2]
            );
            return;

        case PetMarshaller.SEND_CHAT:
            ((PetProvider)provider).sendChat(
                source, ((Integer)args[0]).intValue(), ((Integer)args[1]).intValue(), (String)args[2], (InvocationService.ConfirmListener)args[3]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
