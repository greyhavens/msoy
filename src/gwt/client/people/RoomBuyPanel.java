//
// $Id$

package client.people;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.PurchaseResult;

import com.threerings.msoy.room.gwt.RoomInfo;
import com.threerings.msoy.room.gwt.WebRoomService;
import com.threerings.msoy.room.gwt.WebRoomServiceAsync;

import client.money.BuyPanel;
import client.util.Link;

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

    @Override
    protected void addPurchasedUI (RoomInfo room, FlowPanel boughtPanel)
    {
        boughtPanel.add(new Label(_msgs.boughtRoom()));
        boughtPanel.add(Link.create(_msgs.boughtRoomGo(), Pages.WORLD, "s" + room.sceneId));
    }

    protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);
    protected static final WebRoomServiceAsync _roomsvc = GWT.create(WebRoomService.class);
}
