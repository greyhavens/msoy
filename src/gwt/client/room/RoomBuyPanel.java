//
// $Id$

package client.room;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.PriceQuote;

import com.threerings.msoy.room.gwt.RoomInfo;
import com.threerings.msoy.room.gwt.WebRoomService;
import com.threerings.msoy.room.gwt.WebRoomServiceAsync;

import client.util.ServiceUtil;

import client.money.BuyPanel;

/**
 * A buypanel for purchasing rooms.
 */
public class RoomBuyPanel extends BuyPanel<WebRoomService.RoomPurchaseResult>
{
    /**
     * @param callback: optional, called when the room's been purchased.
     */
    public RoomBuyPanel (PriceQuote quote, AsyncCallback<RoomInfo> callback)
    {
        super(quote);
        _callback = callback;
    }

    @Override
    protected void makePurchase (
        Currency currency, int amount, AsyncCallback<WebRoomService.RoomPurchaseResult> listener)
    {
        _roomsvc.purchaseRoom(currency, amount, listener);
    }

    @Override
    protected void addPurchasedUI (
        WebRoomService.RoomPurchaseResult result, Currency currency, FlowPanel boughtPanel)
    {
        if (_callback != null) {
            _callback.onSuccess(result.newRoom);
        }

        // TODO: more?
    }

    protected AsyncCallback<RoomInfo> _callback;

    protected static final WebRoomServiceAsync _roomsvc = (WebRoomServiceAsync)
        ServiceUtil.bind(GWT.create(WebRoomService.class), WebRoomService.ENTRY_POINT);
}
