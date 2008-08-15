//
// $Id$

package com.threerings.msoy.room.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The asynchronous (client-side) version of {@link WebRoomService}.
 */
public interface WebRoomServiceAsync
{
    /**
     * The asynchronous version of {@link WebRoomService#loadRoomInfo}.
     */
    void loadRoomInfo (int sceneId, AsyncCallback<RoomInfo> callback);

    /**
     * The asynchronous version of {@link WebRoomService#loadMyRooms}.
     */
    void loadMyRooms (AsyncCallback<List<RoomInfo>> callback);

    /**
     * The asynchronous version of {@link WebRoomService.loadGroupRooms}
     */
    void loadGroupRooms (int groupId, AsyncCallback<WebRoomService.RoomsResult> callback);
}
