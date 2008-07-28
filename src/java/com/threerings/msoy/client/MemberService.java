//
// $Id$

package com.threerings.msoy.client;

import com.threerings.msoy.data.all.ReferralInfo;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

/**
 * Services for members.
 */
public interface MemberService extends InvocationService
{
    /**
     * Request to invite the specified user to be our friend.
     */
    void inviteToBeFriend (Client client, int friendId, ConfirmListener listener);

    /**
     * Boot the specified user from the current place.
     */
    void bootFromPlace (Client client, int booteeId, ConfirmListener listener);

    /**
     * Request to know the home scene id for the specified owner.
     */
    void getHomeId (Client client, byte ownerType, int ownerId, ResultListener listener);

    /**
     * Request to get the current MemberLocation of the specified member.
     *
     * Note: presently the member must be a friend.
     */
    void getCurrentMemberLocation (Client client, int memberId, ResultListener listener);

    /**
     * Updates this member's availability status.
     */
    void updateAvailability (Client client, int availability);

    /**
     * Set this client as being "away".
     */
    void setAway (Client client, boolean away, String message);

    /**
     * Invites the specified member to follow the caller. Passing 0 for the memberId will clear all
     * of the caller's followers.
     */
    void inviteToFollow (Client client, int memberId, ConfirmListener listener);

    /**
     * Requests to follow the specified member who must have previously issued an invitation to the
     * caller to follow them. Passing 0 for memberId will clear the caller's following status.
     */
    void followMember (Client client, int memberId, ConfirmListener listener);

    /**
     * Set the avatar in use by this user.
     *
     * @param newScale a new scale for the avatar, or 0 to use the last scale.
     */
    void setAvatar (Client client, int avatarId, float newScale, ConfirmListener listener);

    /**
     * Set the display name for this user.
     */
    void setDisplayName (Client client, String name, InvocationListener listener);

    /**
     * Get the display name for the indicated user.
     */
    void getDisplayName (Client client, int memberId, ResultListener listener);

    /**
     * Get the name of the indicated group.
     */
    void getGroupName (Client client, int groupId, ResultListener listener);

    /**
     * Set the given scene as the owner's home scene
     */
    void setHomeSceneId (Client client, int ownerType, int ownerId, int sceneId,
                         ConfirmListener listener);

    /**
     * Get the given group's home scene id.
     */
    void getGroupHomeSceneId (Client client, int groupId, ResultListener listener);

    /**
     * Acknowledges that the user has read their warning message.
     */
    void acknowledgeWarning (Client client);

    /**
     * Posts a complaint event in underwire.
     */
    void complainMember (Client client, int memberId, String complaint);

    /**
     * Updates the status/headline for this member.
     */
    void updateStatus (Client client, String status, InvocationListener listener);

    /**
     * Tracks the creation of a new referral info structure, for a new visitor.
     */
    void trackReferralCreation (Client client, ReferralInfo info);
}
