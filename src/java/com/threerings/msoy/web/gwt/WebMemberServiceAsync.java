//
// $Id$

package com.threerings.msoy.web.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.data.all.VisitorInfo;


/**
 * The asynchronous (client-side) version of {@link WebMemberService}.
 */
public interface WebMemberServiceAsync
{
    /**
     * The asynchronous version of {@link WebMemberService#getMemberCard}.
     */
    void getMemberCard (int memberId, AsyncCallback<MemberCard> callback);

    /**
     * The asynchronous version of {@link WebMemberService#getFriendStatus}.
     */
    void getFriendStatus (int memberId, AsyncCallback<Boolean> callback);

    /**
     * The asynchronous version of {@link WebMemberService#loadFriends}.
     */
    void loadFriends (int memberId, boolean padWithGreeters,
                      AsyncCallback<WebMemberService.FriendsResult> callback);

    /**
     * The asynchronous version of {@link WebMemberService#addFriend}.
     */
    void addFriend (int friendId, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link WebMemberService#removeFriend}.
     */
    void removeFriend (int friendId, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link WebMemberService#isAutomaticFriender}.
     */
    void isAutomaticFriender (int friendId, AsyncCallback<Boolean> callback)
        throws ServiceException;

    /**
     * The asynchronous version of {@link WebMemberService#getInvitation}.
     */
    void getInvitation (String inviteId, boolean viewing, AsyncCallback<Invitation> callback);

    /**
     * The asynchronous version of {@link WebMemberService#optOut}.
     */
    void optOut (String inviteId, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link WebMemberService#optOutAnnounce}.
     */
    void optOutAnnounce (int memberId, String hash, AsyncCallback<String> callback);

    /**
     * The asynchronous version of {@link WebMemberService#getLeaderList}.
     */
    void getLeaderList (AsyncCallback<List<MemberCard>> callback);

    /**
     * The asynchronous version of {@link WebMemberService#getABTestGroup}.
     */
    void getABTestGroup (VisitorInfo info, String testName, boolean logEvent,
                         AsyncCallback<Integer> callback);

    /**
     * The asynchronous version of {@link WebMemberService#trackClientAction}.
     */
    void trackClientAction (VisitorInfo info, String actionName, String details,
                            AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link WebMemberService#trackTestAction}.
     */
    void trackTestAction (VisitorInfo info, String actionName, String testName,
                          AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link WebMemberService#trackVisitorInfoCreation}.
     */
    void trackVisitorInfoCreation (VisitorInfo info, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link WebMemberService#trackVectorAssociation}.
     */
    void trackVectorAssociation (VisitorInfo info, String vector, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link WebMemberService#trackHttpReferrerAssociation}.
     */
    void trackHttpReferrerAssociation (VisitorInfo info, String referrer,
                                       AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link WebMemberService#trackSessionStatusChange}.
     */
    void trackSessionStatusChange (VisitorInfo info, boolean guest, boolean newInfo,
                                   AsyncCallback<Void> callback);

    /**
     * Small debugging function, writes to text logs on the server. TODO: remove me.
     */
    void debugLog (String stage, String token, String vector, AsyncCallback<Void> callback);
}
