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
     * Handles a {@link PartyService#assignLeader} request.
     */
    void assignLeader (ClientObject caller, int arg1, InvocationService.InvocationListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link PartyService#bootMember} request.
     */
    void bootMember (ClientObject caller, int arg1, InvocationService.InvocationListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link PartyService#inviteMember} request.
     */
    void inviteMember (ClientObject caller, int arg1, InvocationService.InvocationListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link PartyService#leaveParty} request.
     */
    void leaveParty (ClientObject caller, InvocationService.ConfirmListener arg1)
        throws InvocationException;

    /**
     * Handles a {@link PartyService#updateNameOrStatus} request.
     */
    void updateNameOrStatus (ClientObject caller, String arg1, boolean arg2, InvocationService.InvocationListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link PartyService#updateRecruitment} request.
     */
    void updateRecruitment (ClientObject caller, byte arg1, InvocationService.InvocationListener arg2)
        throws InvocationException;
}
