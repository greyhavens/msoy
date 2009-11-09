/**
 *
 */
package client.groups;

import client.money.BuyPanel;
import client.ui.BorderedPopup;
import client.ui.MsoyUI;
import client.util.InfoCallback;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.threerings.msoy.data.all.Theme;
import com.threerings.msoy.group.gwt.GroupService;
import com.threerings.msoy.group.gwt.GroupServiceAsync;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.PriceQuote;
import com.threerings.msoy.money.data.all.PurchaseResult;

public class ThemeBuyPanel extends BuyPanel<Theme>
{
    public static PopupPanel buyTheme (int groupId, String name, final AsyncCallback<Theme> callback)
    {
        final BorderedPopup popup = new BorderedPopup(false, false);

        FlowPanel ui = MsoyUI.createFlowPanel("buyTheme",
            MsoyUI.createLabel(_msgs.ctBuy(name), null),
            new ThemeBuyPanel(groupId, callback));

        popup.setWidget(ui);
        popup.show();

        return popup;
    }

    protected ThemeBuyPanel (int groupId, final AsyncCallback<Theme> callback)
    {
        _groupId = groupId;

        _groupsvc.quoteCreateTheme(new InfoCallback<PriceQuote>(ThemeBuyPanel.this) {
            public void onSuccess (PriceQuote quote) {
                init(quote, callback);
            }
        });
    }

    @Override protected boolean makePurchase (Currency currency, int amount,
        AsyncCallback<PurchaseResult<Theme>> listener)
    {
        _groupsvc.createTheme(_groupId, currency, amount, listener);
        return true;
    }

    protected int _groupId;

    protected static final GroupsMessages _msgs = GWT.create(GroupsMessages.class);
    protected static final GroupServiceAsync _groupsvc = GWT.create(GroupService.class);
}