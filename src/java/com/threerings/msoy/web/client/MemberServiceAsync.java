//
// $Id$

package com.threerings.msoy.web.client;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.web.data.WebIdent;
import com.threerings.msoy.web.data.Invitation;

/**
 * The asynchronous (client-side) version of {@link MemberService}.
 */
public interface MemberServiceAsync
{
    /**
     * The asynchronous version of {@link MemberService#getFriendStatus}.
     */
    public void getFriendStatus (WebIdent ident, int memberId, AsyncCallback callback);

    /**
     * The asynchronous version of {@link MemberService#acceptFriend}.
     */
    public void addFriend (WebIdent ident, int friendId, AsyncCallback callback);

    /**
     * The asynchronous version of {@link MemberService#declineFriend}.
     */
    public void removeFriend (WebIdent ident, int friendId, AsyncCallback callback);

    /**
     * The asynchronous version of {@link MemberService#loadInventory}.
     */
    public void loadInventory (WebIdent ident, byte type, int suiteId, AsyncCallback callback);

    /**
     * The asynchronous version of {@link MemberService#getInvitationsStatus}.
     */
    public void getInvitationsStatus (WebIdent ident, AsyncCallback callback);

    /**
     * The asynchronous version of {@link MemberService#sendInvites}.
     */
    public void sendInvites (WebIdent ident, List addresses, String fromName, String customMessage,
                             boolean anonymous, AsyncCallback callback);

    /**
     * The asynchronous version of {@link MemberService#getInvitation}.
     */
    public void getInvitation (String inviteId, boolean viewing, AsyncCallback callback);

    /**
     * The asynchronous version of {@link MemberService#optOut}.
     */
    public void optOut (Invitation invite, AsyncCallback callback);
}
