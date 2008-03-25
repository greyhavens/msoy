//
// $Id$

package com.threerings.msoy.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

import com.threerings.msoy.data.all.MemberName;

/**
 * Services for members.
 */
public interface MemberService extends InvocationService
{
    /**
     * Request to invite the specified user to be our friend.
     */
    public void inviteToBeFriend (Client client, int friendId, ConfirmListener listener);

    /**
     * Request to know the home scene id for the specified owner.
     */
    public void getHomeId (Client client, byte ownerType, int ownerId, ResultListener listener);

    /**
     * Request to get the current MemberLocation of the specified member.
     *
     * Note: presently the member must be a friend.
     */
    public void getCurrentMemberLocation (Client client, int memberId, ResultListener listener);

    /**
     * Updates this member's availability status.
     */
    public void updateAvailability (Client client, int availability);

    /**
     * Set this client as being "away".
     */
    public void setAway (Client client, boolean away, String message);

    /**
     * Invites the specified member to follow the caller. Passing 0 for the memberId will clear all
     * of the caller's followers.
     */
    public void inviteToFollow (Client client, int memberId, ConfirmListener listener);

    /**
     * Requests to follow the specified member who must have previously issued an invitation to the
     * caller to follow them. Passing 0 for memberId will clear the caller's following status.
     */
    public void followMember (Client client, int memberId, ConfirmListener listener);

    /**
     * Set the avatar in use by this user.
     *
     * @param newScale a new scale for the avatar, or 0 to use the last scale.
     */
    public void setAvatar (Client client, int avatarId, float newScale,
        ConfirmListener listener);

    /**
     * Set the display name for this user.
     */
    public void setDisplayName (Client client, String name, InvocationListener listener);

    /**
     * Get the display name for the indicated user.
     */
    public void getDisplayName (Client client, int memberId, ResultListener listener);

    /**
     * Get the name of the indicated group.
     */
    public void getGroupName (Client client, int groupId, ResultListener listener);

    /**
     * Acknowledge that the specified notification was processed, and can be removed
     * from the user's queue.
     */
    public void acknowledgeNotifications (Client client, int[] ids, InvocationListener listener);

    /**
     * Issue an invitation to the given guest from the user's available invites.
     */
    public void issueInvitation (Client client, MemberName guest, ResultListener listener);

    /**
     * Set the given scene as the member's home scene
     */
    public void setHomeSceneId (Client client, int sceneId, ConfirmListener listener);

    /**
     * Acknowledges that the user has read their warning message.
     */
    public void acknowledgeWarning (Client client);

    /**
     * Posts a complaint event in underwire.
     */
    public void complainMember (Client client, int memberId, String complaint);
}
