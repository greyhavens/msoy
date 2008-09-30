//
// $Id$

package com.threerings.msoy.room.gwt;

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
     * The asynchronous version of {@link WebRoomService#loadMemberRooms}.
     */
    void loadMemberRooms (int memberId, AsyncCallback<WebRoomService.MemberRoomsResult> callback);

    /**
     * The asynchronous version of {@link WebRoomService#loadGroupRooms}
     */
    void loadGroupRooms (int groupId, AsyncCallback<WebRoomService.RoomsResult> callback);
}
