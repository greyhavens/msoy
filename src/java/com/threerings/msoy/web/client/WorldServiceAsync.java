//
// $Id$

package com.threerings.msoy.web.client;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.web.client.WorldService;

import com.threerings.msoy.web.data.LandingData;
import com.threerings.msoy.web.data.LaunchConfig;
import com.threerings.msoy.web.data.MyWhirledData;
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
    public void getLandingData (AsyncCallback<LandingData> callback);

    /**
     * The asynchronous version of {@link WorldService#serializePopularPlaces}.
     */
    public void serializePopularPlaces (WebIdent ident, int n, AsyncCallback<String> callback);

    /**
     * The asynchronous version of {@link WorldService#getMyWhirled}.
     */
    public void getMyWhirled (WebIdent ident, AsyncCallback<MyWhirledData> callback);

    /**
     * The asynchronous version of {@link WorldService#updateWhirledNews}.
     */
    public void updateWhirledNews (WebIdent ident, String newsHtml, AsyncCallback callback);

    /**
     * The asynchronous version of {@link WorldService#loadMyRooms}.
     */
    public void loadMyRooms (WebIdent ident, AsyncCallback<List<WorldService.Room>> callback);

    /**
     * The asynchronous version of {@link WorldService#loadFeed}.
     */
    public void loadFeed (WebIdent ident, int cutoffDays, AsyncCallback callback);

    /**
     * The asynchronous version of {@link WorldService#loadLaunchConfig}.
     */
    public void loadLaunchConfig (WebIdent ident, int gameId, AsyncCallback<LaunchConfig> callback);

    /**
     * The asynchronous version of {@link WorldService#loadRoomInfo}.
     */
    public void loadRoomInfo (int sceneId, AsyncCallback<RoomInfo> callback);
}
