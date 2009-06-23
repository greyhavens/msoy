//
// $Id$

package com.threerings.msoy.web.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.gwt.util.PagedResult;

import com.threerings.msoy.data.all.Friendship;
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
     * The asynchronous version of {@link WebMemberService#getFriendship}.
     */
    void getFriendship (int memberId, AsyncCallback<Friendship> callback);

    /**
     * The asynchronous version of {@link WebMemberService#loadFriends}.
     */
    void loadFriends (int memberId, boolean padWithGreeters,
                      AsyncCallback<WebMemberService.FriendsResult> callback);

    /**
     * The asynchronous version of {@link WebMemberService#loadGreeters}.
     */
    void loadGreeters (int offset, int limit,
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
    void isAutomaticFriender (int friendId, AsyncCallback<Boolean> callback);

    /**
     * The asynchronous version of {@link WebMemberService#loadMutelist}.
     */
    void loadMutelist (int memberId, int offset, int limit,
        AsyncCallback<PagedResult<MemberCard>> callback);

    /**
     * The asynchronous version of {@link WebMemberService#isAutomaticFriender}.
     */
    void setMuted (int memberId, int muteeId, boolean muted, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link WebMemberService#getInvitation}.
     */
    void getInvitation (String inviteId, boolean viewing, AsyncCallback<Invitation> callback);

    /**
     * The asynchronous version of {@link WebMemberService#getGameInvitation}.
     */
    void getGameInvitation (String inviteId, AsyncCallback<Invitation> callback);

    /**
     * The asynchronous version of {@link WebMemberService#optOut}.
     */
    void optOut (boolean gameInvite, String inviteId, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link WebMemberService#optOutAnnounce}.
     */
    void optOutAnnounce (int memberId, String hash, AsyncCallback<String> callback);

    /**
     * The asynchronous version of {@link WebMemberService#getLeaderList}.
     */
    void getLeaderList (AsyncCallback<List<MemberCard>> callback);

    /**
     * The asynchronous version of {@link WebMemberService#noteNewVisitor}.
     */
    void noteNewVisitor (VisitorInfo info, String pageToken, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link WebMemberService#getABTestGroup}.
     */
    void getABTestGroup (VisitorInfo info, String testName, boolean logEvent,
                         AsyncCallback<Integer> callback);

    /**
     * The asynchronous version of {@link WebMemberService#logLandingABTestGroup}.
     */
    void logLandingABTestGroup (VisitorInfo info, String testName, int group,
        AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link WebMemberService#trackTestAction}.
     */
    void trackTestAction (String test, String action, VisitorInfo info,
                          AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link WebMemberService#getFacebookTemplate}.
     */
    void getFacebookTemplate (String code, AsyncCallback<FacebookTemplateCard> callback);
}
