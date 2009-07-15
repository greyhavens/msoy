//
// $Id$

package com.threerings.msoy.facebook.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The asynchronous (client-side) version of {@link FacebookService}.
 */
public interface FacebookServiceAsync
{
    /**
     * The asynchronous version of {@link FacebookService#getAppFriendsInfo}.
     */
    void getAppFriendsInfo (AsyncCallback<List<FacebookFriendInfo>> callback);

    /**
     * The asynchronous version of {@link FacebookService#getGameFriendsInfo}.
     */
    void getGameFriendsInfo (int gameId, AsyncCallback<List<FacebookFriendInfo>> callback);

    /**
     * The asynchronous version of {@link FacebookService#getFriendsUsingApp}.
     */
    void getFriendsUsingApp (AsyncCallback<List<Long>> callback);
}
