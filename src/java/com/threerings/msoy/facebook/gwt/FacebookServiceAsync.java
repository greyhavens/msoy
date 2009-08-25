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
     * The asynchronous version of {@link FacebookService#getTemplate}.
     */
    void getTemplate (String code, AsyncCallback<FacebookTemplateCard> callback);

    /**
     * The asynchronous version of {@link FacebookService#trophyPublished}.
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
        FacebookGame game, boolean appOnly, AsyncCallback<StoryFields> callback);

    /**
     * The asynchronous version of {@link FacebookService#getStoryFields}.
     */
    void getStoryFields (
        FacebookGame game, AsyncCallback<StoryFields> callback);
}
