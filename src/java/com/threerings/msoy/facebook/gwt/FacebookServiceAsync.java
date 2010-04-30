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
     * The async version of {@link FacebookService#getStoryFields}.
     */
    void getStoryFields (FacebookService.StoryKey key, AsyncCallback<FacebookService.StoryFields> callback);

    /**
     * The async version of {@link FacebookService#trackStoryPosted}.
     */
    void trackStoryPosted (FacebookService.StoryKey key, String ident, String trackingId, AsyncCallback<Void> callback);

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
     * The async version of {@link FacebookService#trackPageRequest}.
     */
    void trackPageRequest (int appId, String page, AsyncCallback<Void> callback);
}
