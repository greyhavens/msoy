//
// $Id$

package com.threerings.msoy.room.gwt;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.web.data.WebIdent;

/**
 * The asynchronous (client-side) version of {@link WebRoomService}.
 */
public interface WebRoomServiceAsync
{
    /**
     * The asynchronous version of {@link WebRoomService#serializePopularPlaces}.
     */
    void serializePopularPlaces (WebIdent ident, int n, AsyncCallback<String> callback);

    /**
     * The asynchronous version of {@link WebRoomService#loadRoomInfo}.
     */
    void loadRoomInfo (int sceneId, AsyncCallback<RoomInfo> callback);
}
