//
// $Id$

package com.threerings.msoy.party.server;

import com.threerings.msoy.party.client.PartyService;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link PartyService}.
 */
public interface PartyProvider extends InvocationProvider
{
    /**
     * Handles a {@link PartyService#joinParty} request.
     */
    void joinParty (ClientObject caller, int arg1, InvocationService.ResultListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link PartyService#leaveParty} request.
     */
    void leaveParty (ClientObject caller, InvocationService.ConfirmListener arg1)
        throws InvocationException;

    /**
     * Handles a {@link PartyService#startParty} request.
     */
    void startParty (ClientObject caller, String arg1, InvocationService.ResultListener arg2)
        throws InvocationException;
}
