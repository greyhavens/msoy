//
// $Id$

package client.shop;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.gwt.CatalogListing;

import client.ui.BorderedDialog;
import client.ui.MsoyUI;

public class BuyRemixDialog extends BorderedDialog
{
    /**
     * @param listing the listing for which we'll prompt a buy.
     * @param callback Called with item = null if the user closed the buy dialog, or the
     * item on success.
     */
    public BuyRemixDialog (CatalogListing listing, AsyncCallback<Item> callback)
    {
        super(false, false, false);
        setHeaderTitle(_msgs.buyRemixTitle());
        _callback = callback;

        VerticalPanel panel = new VerticalPanel();
        panel.addStyleName("buyRemixPanel");
        panel.add(MsoyUI.createLabel(_msgs.buyRemixPrompt(), null));

        panel.add(new BuyPanel(listing, new AsyncCallback<Item>() {
            public void onFailure (Throwable cause) { /* unused */ }
            public void onSuccess (Item item) {
                hide();
                // pass the buck
                _callback.onSuccess(item);
            }
        }));
        setContents(panel);
        show();
    }

    @Override protected void onClosed (boolean autoClosed)
    {
        _callback.onSuccess(null);
    }

    protected AsyncCallback<Item> _callback;

    protected static final ShopMessages _msgs = GWT.create(ShopMessages.class);
}
