//
// $Id$

package client.catalog;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.web.CatalogListing;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.web.client.WebContext;

import client.item.BaseItemDetailPopup;

/**
 * Displays a popup detail view of an item from the catalog.
 */
public class ListingDetailPopup extends BaseItemDetailPopup
{
    public ListingDetailPopup (WebContext ctx, CatalogListing listing)
    {
        super(ctx, listing.item);
        _listing = listing;
    }

    // @Override // BaseItemDetailPopup
    protected void createInterface (VerticalPanel details, VerticalPanel controls)
    {
        super.createInterface(details, controls);

        // TODO: add cost

        _purchase = new Button("Buy!");
        _purchase.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                _purchase.setEnabled(false);
                purchaseItem();
            }
        });
        controls.add(_purchase);

        controls.add(_status = new Label(""));
    }

    protected void purchaseItem ()
    {
        _ctx.catalogsvc.purchaseItem(_ctx.creds, _item.getIdent(), new AsyncCallback() {
            public void onSuccess (Object result) {
                _status.setText("Item purchased.");
            }
            public void onFailure (Throwable caught) {
                String reason = caught.getMessage();
                _status.setText("Item purchase failed: " + reason);
                _purchase.setEnabled(true);
            }
        });
    }

    protected CatalogListing _listing;
    protected Button _purchase;
    protected Label _status;
}
