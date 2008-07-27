//
// $Id$

package com.threerings.msoy.world.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;


import com.threerings.msoy.game.gwt.LaunchConfig;
import com.threerings.msoy.group.gwt.MyWhirledData;
import com.threerings.msoy.person.gwt.FeedMessage;

import com.threerings.msoy.web.data.LandingData;
import com.threerings.msoy.web.data.RoomInfo;
import com.threerings.msoy.web.data.WebIdent;

/**
 * The asynchronous (client-side) version of {@link WorldService}.
 */
public interface WorldServiceAsync
{
    /**
     * The asynchronous version of {@link WorldService#getLandingData}.
     */
    void getLandingData (AsyncCallback<LandingData> callback);

    /**
     * The asynchronous version of {@link WorldService#serializePopularPlaces}.
     */
    void serializePopularPlaces (WebIdent ident, int n, AsyncCallback<String> callback);

    /**
     * The asynchronous version of {@link WorldService#getMyWhirled}.
     */
    void getMyWhirled (WebIdent ident, AsyncCallback<MyWhirledData> callback);

    /**
     * The asynchronous version of {@link WorldService#updateWhirledNews}.
     */
    void updateWhirledNews (WebIdent ident, String newsHtml, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link WorldService#loadMyRooms}.
     */
    void loadMyRooms (WebIdent ident, AsyncCallback<List<WorldService.Room>> callback);

    /**
     * The asynchronous version of {@link WorldService#loadFeed}.
     */
    public void loadFeed (WebIdent ident, int cutoffDays,
                          AsyncCallback<List<FeedMessage>> callback);

    /**
     * The asynchronous version of {@link WorldService#loadLaunchConfig}.
     */
    void loadLaunchConfig (WebIdent ident, int gameId, AsyncCallback<LaunchConfig> callback);

    /**
     * The asynchronous version of {@link WorldService#loadRoomInfo}.
     */
    void loadRoomInfo (int sceneId, AsyncCallback<RoomInfo> callback);
}
