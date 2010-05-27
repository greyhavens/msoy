//
// $Id$

package com.threerings.msoy.room.gwt;

import java.util.List;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.threerings.msoy.data.all.RatingResult;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.PurchaseResult;

/**
 * Provides the asynchronous version of {@link WebRoomService}.
 */
public interface WebRoomServiceAsync
{
    /**
     * The async version of {@link WebRoomService#loadRoomDetail}.
     */
    void loadRoomDetail (int sceneId, AsyncCallback<RoomDetail> callback);

    /**
     * The async version of {@link WebRoomService#canGiftRoom}.
     */
    void canGiftRoom (int sceneId, AsyncCallback<Void> callback);

    /**
     * The async version of {@link WebRoomService#loadMemberRooms}.
     */
    void loadMemberRooms (int memberId, AsyncCallback<WebRoomService.MemberRoomsResult> callback);

    /**
     * The async version of {@link WebRoomService#loadGroupRooms}.
     */
    void loadGroupRooms (int groupId, AsyncCallback<WebRoomService.RoomsResult> callback);

    /**
     * The async version of {@link WebRoomService#rateRoom}.
     */
    void rateRoom (int sceneId, byte rating, AsyncCallback<RatingResult> callback);

    /**
     * The async version of {@link WebRoomService#loadOverview}.
     */
    void loadOverview (AsyncCallback<WebRoomService.OverviewResult> callback);

    /**
     * The async version of {@link WebRoomService#loadDesignWinners}.
     */
    void loadDesignWinners (AsyncCallback<List<RoomInfo>> callback);

    /**
     * The async version of {@link WebRoomService#purchaseRoom}.
     */
    void purchaseRoom (Currency currency, int authedCost, AsyncCallback<PurchaseResult<RoomInfo>> callback);

    /**
     * The async version of {@link WebRoomService#stampRoom}.
     */
    void stampRoom (int sceneId, int groupId, boolean doStamp, AsyncCallback<Void> callback);

    /**
     * The async version of {@link WebRoomService#makeTemplate}.
     */
    void makeTemplate (int sceneId, int groupId, boolean doMake, AsyncCallback<Void> callback);
}
