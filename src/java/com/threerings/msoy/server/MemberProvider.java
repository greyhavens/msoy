//
// $Id$

package com.threerings.msoy.server;

import com.threerings.msoy.client.MemberService;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link MemberService}.
 */
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
     * Handles a {@link MemberService#ditchFollower} request.
     */
    void ditchFollower (ClientObject caller, int arg1, InvocationService.InvocationListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link MemberService#followMember} request.
     */
    void followMember (ClientObject caller, int arg1, InvocationService.InvocationListener arg2)
        throws InvocationException;

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
     * Handles a {@link MemberService#getHomeId} request.
     */
    void getHomeId (ClientObject caller, byte arg1, int arg2, InvocationService.ResultListener arg3)
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
     * Handles a {@link MemberService#inviteToFollow} request.
     */
    void inviteToFollow (ClientObject caller, int arg1, InvocationService.InvocationListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link MemberService#setAvatar} request.
     */
    void setAvatar (ClientObject caller, int arg1, InvocationService.ConfirmListener arg2)
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
     * Handles a {@link MemberService#setHomeSceneId} request.
     */
    void setHomeSceneId (ClientObject caller, int arg1, int arg2, int arg3, InvocationService.ConfirmListener arg4)
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
