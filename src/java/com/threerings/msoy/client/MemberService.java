//
// $Id$

package com.threerings.msoy.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

/**
 * Services for members.
 */
public interface MemberService extends InvocationService
{
    /**
     * Request to invite the specified user to be our friend. The result returned is a Boolean
     * value indicating whether the friend invitation was automatically accepted. Otherwise, it was
     * sent via email.
     */
    void inviteToBeFriend (Client client, int friendId, ResultListener listener);

    /**
     * Requests to invite a set of users to be our friend.
     * TODO: a custom listener with more details might be a good idea
     */
    void inviteAllToBeFriends (Client client, int[] memberIds, ConfirmListener listener);

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
     * Set this client as being "away".
     *
     * @param message the away message, or null to be back.
     */
    void setAway (Client client, String message, ConfirmListener listener);

    /**
     * Set the specified member as muted.
     */
    void setMuted (Client client, int muteeId, boolean muted, ConfirmListener listener);

    /**
     * Invites the specified member to follow the caller. Passing 0 for the memberId will clear all
     * of the caller's followers.
     */
    void inviteToFollow (Client client, int memberId, InvocationListener listener);

    /**
     * Requests to follow the specified member who must have previously issued an invitation to the
     * caller to follow them. Passing 0 for memberId will clear the caller's following status.
     */
    void followMember (Client client, int memberId, InvocationListener listener);

    /**
     * Removes a player from the caller's list of followers. Passing 0 for memberId will clear all
     * the caller's followers.
     */
    void ditchFollower (Client client, int memberId, InvocationListener listener);

    /**
     * Set the avatar in use by this user.
     */
    void setAvatar (Client client, int avatarId, ConfirmListener listener);

    /**
     * Set the display name for this user.
     */
    void setDisplayName (Client client, String name, ConfirmListener listener);

    /**
     * Get the display name for the indicated user.
     */
    void getDisplayName (Client client, int memberId, ResultListener listener);

    /**
     * Set the given scene as the owner's home scene
     */
    void setHomeSceneId (Client client, int ownerType, int ownerId, int sceneId,
                         ConfirmListener listener);

    /**
     * Acknowledges that the user has read their warning message.
     */
    void acknowledgeWarning (Client client);

    /**
     * Posts a complaint event in underwire.
     */
    void complainMember (Client client, int memberId, String complaint);

    /**
     * Updates the status/headline for the calling member.
     */
    void updateStatus (Client client, String status, InvocationListener listener);
}
