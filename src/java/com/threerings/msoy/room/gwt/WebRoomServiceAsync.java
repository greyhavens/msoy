//
// $Id$

package com.threerings.msoy.room.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.data.all.RatingResult;

/**
 * The asynchronous (client-side) version of {@link WebRoomService}.
 */
public interface WebRoomServiceAsync
{
    /**
     * The asynchronous version of {@link WebRoomService#loadRoomDetail}.
     */
    void loadRoomDetail (int sceneId, AsyncCallback<RoomDetail> callback);

    /**
     * The asynchronous version of {@link WebRoomService#loadMemberRooms}.
     */
    void loadMemberRooms (int memberId, AsyncCallback<WebRoomService.MemberRoomsResult> callback);

    /**
     * The asynchronous version of {@link WebRoomService#loadGroupRooms}
     */
    void loadGroupRooms (int groupId, AsyncCallback<WebRoomService.RoomsResult> callback);

    /**
     * The asynchronous version of {@link WebRoomService#rateRoom}
     */
    void rateRoom (int sceneId, byte rating, AsyncCallback<RatingResult> callback);

    /**
     * The asynchronous version of {@link WebRoomService#loadOverview}
     */
    void loadOverview (AsyncCallback<WebRoomService.OverviewResult> callback);

    /**
     * The asynchronous version of {@link WebRoomService#loadDesignWinners}
     */
    void loadDesignWinners (AsyncCallback<List<RoomInfo>> callback);
}
