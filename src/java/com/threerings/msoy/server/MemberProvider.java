//
// $Id$

package com.threerings.msoy.server;

import javax.annotation.Generated;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

import com.threerings.msoy.client.MemberService;

/**
 * Defines the server-side of the {@link MemberService}.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from MemberService.java.")
public interface MemberProvider extends InvocationProvider
{
    /**
     * Handles a {@link MemberService#acknowledgeWarning} request.
     */
    void acknowledgeWarning (ClientObject caller);

    /**
     * Handles a {@link MemberService#bootFromPlace} request.
     */
    void bootFromPlace (ClientObject caller, int arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link MemberService#complainMember} request.
     */
    void complainMember (ClientObject caller, int arg1, String arg2);

    /**
     * Handles a {@link MemberService#getCurrentMemberLocation} request.
     */
    void getCurrentMemberLocation (ClientObject caller, int arg1, InvocationService.ResultListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link MemberService#getDisplayName} request.
     */
    void getDisplayName (ClientObject caller, int arg1, InvocationService.ResultListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link MemberService#inviteAllToBeFriends} request.
     */
    void inviteAllToBeFriends (ClientObject caller, int[] arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link MemberService#inviteToBeFriend} request.
     */
    void inviteToBeFriend (ClientObject caller, int arg1, InvocationService.ResultListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link MemberService#setAway} request.
     */
    void setAway (ClientObject caller, String arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link MemberService#setDisplayName} request.
     */
    void setDisplayName (ClientObject caller, String arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link MemberService#setMuted} request.
     */
    void setMuted (ClientObject caller, int arg1, boolean arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link MemberService#updateStatus} request.
     */
    void updateStatus (ClientObject caller, String arg1, InvocationService.InvocationListener arg2)
        throws InvocationException;
}
