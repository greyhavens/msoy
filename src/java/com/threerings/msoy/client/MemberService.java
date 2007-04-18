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
     * Request to add the specified user to the client's friendlist.
     */
    public void alterFriend (Client client, int friendId, boolean add, ConfirmListener listener);

    /**
     * Request to know the home scene id for the specified owner.
     */
    public void getHomeId (Client client, byte ownerType, int ownerId, ResultListener listener);

    /**
     * Set the avatar in use by this user.
     *
     * @param newScale a new scale for the avatar, or 0 to use the last scale.
     */
    public void setAvatar (Client client, int avatarId, float newScale, InvocationListener listener);

    /**
     * Set the display name for this user.
     */
    public void setDisplayName (Client client, String name, InvocationListener listener);

    /**
     * Request to purchase a new room.
     */
    public void purchaseRoom (Client client, ConfirmListener listener);
}
