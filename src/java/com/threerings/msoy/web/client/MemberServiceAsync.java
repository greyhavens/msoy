//
// $Id$

package com.threerings.msoy.web.client;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.threerings.msoy.web.data.WebCreds;

/**
 * The asynchronous (client-side) version of {@link MemberService}.
 */
public interface MemberServiceAsync
{
    /**
     * The asynchronous version of {@link MemberService#getFriendStatus}.
     */
    public void getFriendStatus (WebCreds creds, int memberId, AsyncCallback callback);

    /**
     * The asynchronous version of {@link MemberService#acceptFriend}.
     */
    public void addFriend (WebCreds creds, int friendId, AsyncCallback callback);

    /**
     * The asynchronous version of {@link MemberService#declineFriend}.
     */
    public void removeFriend (WebCreds creds, int friendId, AsyncCallback callback);

    /**
     * The asynchronous version of {@link MemberService#loadInventory}.
     */
    public void loadInventory (WebCreds creds, byte type, AsyncCallback callback);

    /**
     * The asynchronous version of {@link MemberService#serializeNeighborhood}.
     */
    public void serializeNeighborhood (WebCreds creds, int entityId, boolean forGroup,
                                       AsyncCallback callback);

    /**
     * The asynchronous version of {@link MemberService#serializePopularPlaces}.
     */
    public void serializePopularPlaces (WebCreds creds, int n, AsyncCallback callback);

    /**
     * The asynchronous version of {@link MemberService#getInvitationStatus}.
     */
    public void getInvitationsStatus (WebCreds creds, AsyncCallback callback);

    /**
     * The asynchronous version of {@link MemberService#sendInvites}.
     */
    public void sendInvites (WebCreds creds, List addresses, String customMessage, 
        AsyncCallback callback);

    /**
     * The asynchronous version of {@link MemberService#getInvitation}.
     */
    public void getInvitation (String inviteId, AsyncCallback callback);
}
