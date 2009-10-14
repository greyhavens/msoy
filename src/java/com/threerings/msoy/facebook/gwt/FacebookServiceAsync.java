//
// $Id$

package com.threerings.msoy.facebook.gwt;

import java.util.List;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Provides the asynchronous version of {@link FacebookService}.
 */
public interface FacebookServiceAsync
{
    /**
     * The async version of {@link FacebookService#getTrophyStoryFields}.
     */
    void getTrophyStoryFields (int appId, int gameId, AsyncCallback<FacebookService.StoryFields> callback);

    /**
     * The async version of {@link FacebookService#trophyPublished}.
     */
    void trophyPublished (int appId, int gameId, String ident, String trackingId, AsyncCallback<Void> callback);

    /**
     * The async version of {@link FacebookService#getAppFriendsInfo}.
     */
    void getAppFriendsInfo (int appId, AsyncCallback<List<FacebookFriendInfo>> callback);

    /**
     * The async version of {@link FacebookService#getGameFriendsInfo}.
     */
    void getGameFriendsInfo (int appId, int gameId, AsyncCallback<List<FacebookFriendInfo>> callback);

    /**
     * The async version of {@link FacebookService#getInviteInfo}.
     */
    void getInviteInfo (int appId, FacebookGame game, AsyncCallback<FacebookService.InviteInfo> callback);

    /**
     * The async version of {@link FacebookService#sendChallengeNotification}.
     */
    void sendChallengeNotification (int appId, FacebookGame game, boolean appOnly, AsyncCallback<FacebookService.StoryFields> callback);

    /**
     * The async version of {@link FacebookService#getChallengeStoryFields}.
     */
    void getChallengeStoryFields (int appId, FacebookGame game, AsyncCallback<FacebookService.StoryFields> callback);

    /**
     * The async version of {@link FacebookService#getLevelUpStoryFields}.
     */
    void getLevelUpStoryFields (int appId, AsyncCallback<FacebookService.StoryFields> callback);

    /**
     * The async version of {@link FacebookService#challengePublished}.
     */
    void challengePublished (int appId, FacebookGame game, String trackingId, AsyncCallback<Void> callback);

    /**
     * The async version of {@link FacebookService#levelUpPublished}.
     */
    void levelUpPublished (int appId, String trackingId, AsyncCallback<Void> callback);

    /**
     * The async version of {@link FacebookService#trackPageRequest}.
     */
    void trackPageRequest (int appId, String page, AsyncCallback<Void> callback);
}
