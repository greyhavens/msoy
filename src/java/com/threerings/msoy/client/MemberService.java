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
     * Request to add the specified user to the client's friendlist.
     */
    public void alterFriend (Client client, int friendId, boolean add, ConfirmListener listener);

    /**
     * Request to know the home scene id for the specified owner.
     */
    public void getHomeId (Client client, byte ownerType, int ownerId, ResultListener listener);

    /**
     * Request to get the current (scene id) for the specified member.
     *
     * Note: presently the member must be a friend.
     */
    public void getCurrentSceneId (Client client, int memberId, ResultListener listener);

    /**
     * Set the avatar in use by this user.
     *
     * @param newScale a new scale for the avatar, or 0 to use the last scale.
     */
    public void setAvatar (Client client, int avatarId, float newScale, 
        InvocationListener listener);

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
}
