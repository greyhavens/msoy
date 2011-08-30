//
// $Id$

package client.shop;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;

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

        panel.add(new ItemBuyPanel(listing, new AsyncCallback<Item>() {
            public void onFailure (Throwable cause) { /* unused */ }
            public void onSuccess (Item item) {
                hide();
                // pass the buck
                _callback.onSuccess(item);
            }
        }));
        setContents(panel);
        addButton(new Button(_cmsgs.cancel(), new ClickHandler() {
            public void onClick (ClickEvent event) {
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
