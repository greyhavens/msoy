//
// $Id$

package client.people;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.PriceQuote;
import com.threerings.msoy.money.data.all.PurchaseResult;

import com.threerings.msoy.room.gwt.RoomInfo;
import com.threerings.msoy.room.gwt.WebRoomService;
import com.threerings.msoy.room.gwt.WebRoomServiceAsync;

import client.util.ServiceUtil;

import client.money.BuyPanel;

/**
 * A buypanel for purchasing rooms.
 */
public class RoomBuyPanel extends BuyPanel<RoomInfo>
{
    @Override
    protected boolean makePurchase (
        Currency currency, int amount, AsyncCallback<PurchaseResult<RoomInfo>> listener)
    {
        _roomsvc.purchaseRoom(currency, amount, listener);
        return true;
    }

    protected static final WebRoomServiceAsync _roomsvc = (WebRoomServiceAsync)
        ServiceUtil.bind(GWT.create(WebRoomService.class), WebRoomService.ENTRY_POINT);
}
