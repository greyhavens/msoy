//
// $Id$

package com.threerings.msoy.facebook.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.threerings.msoy.facebook.gwt.FacebookService.InviteInfo;
import com.threerings.msoy.web.gwt.WebMemberService;

/**
 * The asynchronous (client-side) version of {@link FacebookService}.
 */
public interface FacebookServiceAsync
{
    /**
     * The asynchronous version of {@link WebMemberService#getFacebookTemplate}.
     */
    void getTemplate (String code, AsyncCallback<FacebookTemplateCard> callback);

    /**
     * The asynchronous version of {@link WebMemberService#trophyPublishedToFacebook}.
     */
    void trophyPublished (int gameId, String ident, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link FacebookService#getAppFriendsInfo}.
     */
    void getAppFriendsInfo (AsyncCallback<List<FacebookFriendInfo>> callback);

    /**
     * The asynchronous version of {@link FacebookService#getGameFriendsInfo}.
     */
    void getGameFriendsInfo (int gameId, AsyncCallback<List<FacebookFriendInfo>> callback);

    /**
     * The asynchronous version of {@link FacebookService#getInviteInfo}.
     */
    void getInviteInfo (FacebookGame game, AsyncCallback<InviteInfo> callback);

    /**
     * The asynchronous version of {@link FacebookService#sendChallengeNotification}.
     */
    void sendChallengeNotification (
        FacebookGame game, boolean appOnly, AsyncCallback<Void> callback);
}
