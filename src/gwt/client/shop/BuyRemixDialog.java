//
// $Id$

package client.shop;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.gwt.CatalogListing;

import client.shell.ShellMessages;
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

        FlowPanel panel = new FlowPanel();
        panel.setStyleName("listingDetailPanel");
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
        addButton(new Button(_cmsgs.cancel(), new ClickListener() {
            public void onClick (Widget sender) {
                hide();
                _callback.onSuccess(null);
            }
        }));
        show();
    }

    @Override protected void onClosed (boolean autoClosed)
    {
        _callback.onSuccess(null);
    }

    protected AsyncCallback<Item> _callback;

    protected static final ShopMessages _msgs = GWT.create(ShopMessages.class);
    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
}
