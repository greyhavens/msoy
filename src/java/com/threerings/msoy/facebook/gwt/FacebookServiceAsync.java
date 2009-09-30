//
// $Id$

package com.threerings.msoy.facebook.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.threerings.msoy.facebook.gwt.FacebookService.StoryFields;
import com.threerings.msoy.facebook.gwt.FacebookService.InviteInfo;

/**
 * The asynchronous (client-side) version of {@link FacebookService}.
 */
public interface FacebookServiceAsync
{
    /**
     * The asynchronous version of {@link FacebookService#getTrophyStoryFields}.
     */
    void getTrophyStoryFields (int appId, int gameId, AsyncCallback<StoryFields> callback);

    /**
     * The asynchronous version of {@link FacebookService#trophyPublished}.
     */
    void trophyPublished (int appId, int gameId, String ident, String trackingId,
        AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link FacebookService#getAppFriendsInfo}.
     */
    void getAppFriendsInfo (int appId, AsyncCallback<List<FacebookFriendInfo>> callback);

    /**
     * The asynchronous version of {@link FacebookService#getGameFriendsInfo}.
     */
    void getGameFriendsInfo (int appId, int gameId, AsyncCallback<List<FacebookFriendInfo>> callback);

    /**
     * The asynchronous version of {@link FacebookService#getInviteInfo}.
     */
    void getInviteInfo (int appId, FacebookGame game, AsyncCallback<InviteInfo> callback);

    /**
     * The asynchronous version of {@link FacebookService#sendChallengeNotification}.
     */
    void sendChallengeNotification (
        int appId, FacebookGame game, boolean appOnly, AsyncCallback<StoryFields> callback);

    /**
     * The asynchronous version of {@link FacebookService#getChallengeStoryFields}.
     */
    void getChallengeStoryFields (
        int appId, FacebookGame game, AsyncCallback<StoryFields> callback);

    /**
     * The asynchronous version of {@link FacebookService#getLevelUpStoryFields}.
     */
    void getLevelUpStoryFields (int appId, AsyncCallback<StoryFields> callback);

    /**
     * The asynchronous version of {@link FacebookService#challengePublished}.
     */
    void challengePublished (
        int appId, FacebookGame game, String trackingId, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link FacebookService#levelUpPublished}.
     */
    void levelUpPublished (int appId, String trackingId, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link FacebookService#trackPageRequest}.
     */
    void trackPageRequest (int appId, String page, AsyncCallback<Void> callback);
}
