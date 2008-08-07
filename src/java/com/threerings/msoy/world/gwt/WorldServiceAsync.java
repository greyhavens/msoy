//
// $Id$

package com.threerings.msoy.world.gwt;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.web.data.WebIdent;

/**
 * The asynchronous (client-side) version of {@link WorldService}.
 */
public interface WorldServiceAsync
{
    /**
     * The asynchronous version of {@link WorldService#serializePopularPlaces}.
     */
    void serializePopularPlaces (WebIdent ident, int n, AsyncCallback<String> callback);

    /**
     * The asynchronous version of {@link WorldService#loadRoomInfo}.
     */
    void loadRoomInfo (int sceneId, AsyncCallback<RoomInfo> callback);
}
