//
// $Id$

package com.threerings.msoy.server;

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
    public void acknowledgeWarning (ClientObject caller);

    /**
     * Handles a {@link MemberService#bootFromPlace} request.
     */
    public void bootFromPlace (ClientObject caller, int arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link MemberService#complainMember} request.
     */
    public void complainMember (ClientObject caller, int arg1, String arg2);

    /**
     * Handles a {@link MemberService#followMember} request.
     */
    public void followMember (ClientObject caller, int arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link MemberService#getCurrentMemberLocation} request.
     */
    public void getCurrentMemberLocation (ClientObject caller, int arg1, InvocationService.ResultListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link MemberService#getDisplayName} request.
     */
    public void getDisplayName (ClientObject caller, int arg1, InvocationService.ResultListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link MemberService#getGroupHomeSceneId} request.
     */
    public void getGroupHomeSceneId (ClientObject caller, int arg1, InvocationService.ResultListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link MemberService#getGroupName} request.
     */
    public void getGroupName (ClientObject caller, int arg1, InvocationService.ResultListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link MemberService#getHomeId} request.
     */
    public void getHomeId (ClientObject caller, byte arg1, int arg2, InvocationService.ResultListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link MemberService#inviteToBeFriend} request.
     */
    public void inviteToBeFriend (ClientObject caller, int arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link MemberService#inviteToFollow} request.
     */
    public void inviteToFollow (ClientObject caller, int arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link MemberService#setAvatar} request.
     */
    public void setAvatar (ClientObject caller, int arg1, float arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link MemberService#setAway} request.
     */
    public void setAway (ClientObject caller, boolean arg1, String arg2);

    /**
     * Handles a {@link MemberService#setDisplayName} request.
     */
    public void setDisplayName (ClientObject caller, String arg1, InvocationService.InvocationListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link MemberService#setHomeSceneId} request.
     */
    public void setHomeSceneId (ClientObject caller, int arg1, int arg2, int arg3, InvocationService.ConfirmListener arg4)
        throws InvocationException;

    /**
     * Handles a {@link MemberService#updateAvailability} request.
     */
    public void updateAvailability (ClientObject caller, int arg1);
}
