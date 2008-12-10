//
// $Id$

package com.threerings.msoy.party.server;

import com.threerings.msoy.party.client.PartyBoardService;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link PartyBoardService}.
 */
public interface PartyBoardProvider extends InvocationProvider
{
    /**
     * Handles a {@link PartyBoardService#createParty} request.
     */
    void createParty (ClientObject caller, String arg1, int arg2, boolean arg3, InvocationService.ResultListener arg4)
        throws InvocationException;

    /**
     * Handles a {@link PartyBoardService#getPartyBoard} request.
     */
    void getPartyBoard (ClientObject caller, String arg1, InvocationService.ResultListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link PartyBoardService#getPartyDetail} request.
     */
    void getPartyDetail (ClientObject caller, int arg1, InvocationService.ResultListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link PartyBoardService#joinParty} request.
     */
    void joinParty (ClientObject caller, int arg1, InvocationService.ResultListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link PartyBoardService#locateMyParty} request.
     */
    void locateMyParty (ClientObject caller, InvocationService.ResultListener arg1)
        throws InvocationException;
}
