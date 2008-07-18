//
// $Id$

package com.threerings.msoy.web.client;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.data.all.ReferralInfo;
import com.threerings.msoy.item.data.all.Item;

import com.threerings.msoy.web.data.Invitation;
import com.threerings.msoy.web.data.InvitationResults;
import com.threerings.msoy.web.data.MemberCard;
import com.threerings.msoy.web.data.MemberInvites;
import com.threerings.msoy.web.data.WebIdent;

/**
 * The asynchronous (client-side) version of {@link MemberService}.
 */
public interface MemberServiceAsync
{
    /**
     * The asynchronous version of {@link MemberService#getMemberCard}.
     */
    public void getMemberCard (int memberId, AsyncCallback<MemberCard> callback);

    /**
     * The asynchronous version of {@link MemberService#getFriendStatus}.
     */
    public void getFriendStatus (WebIdent ident, int memberId, AsyncCallback<Boolean> callback);

    /**
     * The asynchronous version of {@link MemberService#acceptFriend}.
     */
    public void addFriend (WebIdent ident, int friendId, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link MemberService#declineFriend}.
     */
    public void removeFriend (WebIdent ident, int friendId, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link MemberService#loadInventory}.
     */
    public void loadInventory (WebIdent ident, byte type, int suiteId, 
                               AsyncCallback<List<Item>> callback);

    /**
     * The asynchronous version of {@link MemberService#getInvitationsStatus}.
     */
    public void getInvitationsStatus (WebIdent ident, AsyncCallback<MemberInvites> callback);

    /**
     * The asynchronous version of {@link MemberService#sendInvites}.
     */
    public void sendInvites (WebIdent ident, List addresses, String fromName, String customMessage,
                             boolean anonymous, AsyncCallback<InvitationResults> callback);

    /**
     * The asynchronous version of {@link MemberService#getInvitation}.
     */
    public void getInvitation (String inviteId, boolean viewing, 
                               AsyncCallback<Invitation> callback);

    /**
     * The asynchronous version of {@link MemberService#removeInvitation}.
     */
    public void removeInvitation (WebIdent ident, String inviteId, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link MemberService#optOut}.
     */
    public void optOut (String inviteId, AsyncCallback<Void> callback);
    
    /**
     * The asynchronous version of {@link MemberService#getLeaderList}.
     */
    public void getLeaderList (AsyncCallback<List<MemberCard>> callback);
    
    /**
     * The asynchronous version of {@link MemberService#getABTestGroup}.
     */
    public void getABTestGroup (
        ReferralInfo info, String testName, boolean logEvent, AsyncCallback<Integer> callback);

    /**
     * The asynchronous version of {@link MemberService#trackClientAction}.
     */
    public void trackClientAction (
        ReferralInfo info, String actionName, String testName, AsyncCallback<Void> callback);
    
}
