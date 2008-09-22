//
// $Id$

package com.threerings.msoy.server;

import com.threerings.msoy.client.MemberService;
import com.threerings.msoy.data.all.ReferralInfo;
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
     * Handles a {@link MemberService#dispatchDeferredNotifications} request.
     */
    void dispatchDeferredNotifications (ClientObject caller);

    /**
     * Handles a {@link MemberService#emailShare} request.
     */
    void emailShare (ClientObject caller, boolean arg1, String arg2, int arg3, String[] arg4, String arg5, InvocationService.ConfirmListener arg6)
        throws InvocationException;

    /**
     * Handles a {@link MemberService#followMember} request.
     */
    void followMember (ClientObject caller, int arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link MemberService#getABTestGroup} request.
     */
    void getABTestGroup (ClientObject caller, ReferralInfo arg1, String arg2, boolean arg3, InvocationService.ResultListener arg4)
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
     * Handles a {@link MemberService#getGroupHomeSceneId} request.
     */
    void getGroupHomeSceneId (ClientObject caller, int arg1, InvocationService.ResultListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link MemberService#getGroupName} request.
     */
    void getGroupName (ClientObject caller, int arg1, InvocationService.ResultListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link MemberService#getHomeId} request.
     */
    void getHomeId (ClientObject caller, byte arg1, int arg2, InvocationService.ResultListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link MemberService#inviteToBeFriend} request.
     */
    void inviteToBeFriend (ClientObject caller, int arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link MemberService#inviteToFollow} request.
     */
    void inviteToFollow (ClientObject caller, int arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link MemberService#loadAllBadges} request.
     */
    void loadAllBadges (ClientObject caller, InvocationService.ResultListener arg1)
        throws InvocationException;

    /**
     * Handles a {@link MemberService#setAvatar} request.
     */
    void setAvatar (ClientObject caller, int arg1, float arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link MemberService#setAway} request.
     */
    void setAway (ClientObject caller, boolean arg1, String arg2);

    /**
     * Handles a {@link MemberService#setDisplayName} request.
     */
    void setDisplayName (ClientObject caller, String arg1, InvocationService.InvocationListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link MemberService#setHomeSceneId} request.
     */
    void setHomeSceneId (ClientObject caller, int arg1, int arg2, int arg3, InvocationService.ConfirmListener arg4)
        throws InvocationException;

    /**
     * Handles a {@link MemberService#trackClientAction} request.
     */
    void trackClientAction (ClientObject caller, ReferralInfo arg1, String arg2, String arg3);

    /**
     * Handles a {@link MemberService#trackReferralCreation} request.
     */
    void trackReferralCreation (ClientObject caller, ReferralInfo arg1);

    /**
     * Handles a {@link MemberService#trackTestAction} request.
     */
    void trackTestAction (ClientObject caller, ReferralInfo arg1, String arg2, String arg3);

    /**
     * Handles a {@link MemberService#updateAvailability} request.
     */
    void updateAvailability (ClientObject caller, int arg1);

    /**
     * Handles a {@link MemberService#updateStatus} request.
     */
    void updateStatus (ClientObject caller, String arg1, InvocationService.InvocationListener arg2)
        throws InvocationException;
}
