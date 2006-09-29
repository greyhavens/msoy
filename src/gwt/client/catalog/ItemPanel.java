//
// $Id$

package client.catalog;

import java.util.ArrayList;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.msoy.item.web.CatalogListing;
import com.threerings.msoy.item.web.ItemGIdent;
import com.threerings.msoy.web.client.WebContext;

/**
 * Displays all items of a particular type int the catalog.
 */
public class ItemPanel extends VerticalPanel
{
    public ItemPanel (WebContext ctx, String type)
    {
        // setStyleName("inventory_item");
        _ctx = ctx;
        _type = type;

        // this will contain our items
        add(_contents = new FlowPanel());

        add(_status = new Label(""));
    }

    protected void onLoad ()
    {
        // load the catalog if we haven't one already
        if (_listings == null) {
            _ctx.catalogsvc.loadCatalog(_ctx.creds, _type, new AsyncCallback() {
                public void onSuccess (Object result) {
                    _listings = (ArrayList)result;
                    if (_listings == null || _listings.size() == 0) {
                        _contents.add(new Label(
                            "There are no " + _type + " items listed."));
                    } else {
                        for (int ii = 0; ii < _listings.size(); ii++) {
                            _contents.add(new ItemContainer(
                                (CatalogListing) _listings.get(ii),
                                ItemPanel.this));
                            }
                    }
                }
                public void onFailure (Throwable caught) {
                    GWT.log("loadCatalog failed", caught);
                    // TODO: if ServiceException, translate
                    _contents.add(new Label("Failed to load catalog."));
                }
            });
        }
    }

    protected void purchaseItem (ItemGIdent item)
    {
        _ctx.catalogsvc.purchaseItem(_ctx.creds, item, new AsyncCallback() {
            public void onSuccess (Object result) {
                setStatus("Item purchased.");
            }
            public void onFailure (Throwable caught) {
                String reason = caught.getMessage();
                setStatus("Item creation failed: " + reason);
            }
        });
    }

    /**
     * Displays a status message to the user, may be called by item editors.
     */
    protected void setStatus (String status)
    {
        _status.setText(status);
    }

    protected WebContext _ctx;

    protected FlowPanel _contents;
    protected Label _status;

    protected String _type;
    protected ArrayList _listings;
}
