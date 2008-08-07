//
// $Id$

package com.threerings.msoy.room.server;

import com.threerings.msoy.room.client.PetService;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link PetService}.
 */
public interface PetProvider extends InvocationProvider
{
    /**
     * Handles a {@link PetService#callPet} request.
     */
    void callPet (ClientObject caller, int arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link PetService#orderPet} request.
     */
    void orderPet (ClientObject caller, int arg1, int arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link PetService#sendChat} request.
     */
    void sendChat (ClientObject caller, int arg1, int arg2, String arg3, InvocationService.ConfirmListener arg4)
        throws InvocationException;
}
