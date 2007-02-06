//
// $Id$

package client.catalog;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.util.DataModel;

import com.threerings.msoy.item.web.CatalogListing;
import com.threerings.msoy.item.web.Item;

/**
 * Displays all items of a particular type in the catalog.
 */
public class ItemPanel extends DockPanel
{
    /** The number of columns of items to display. */
    public static final int COLUMNS = 4;

    /** The number of rows of items to display. */
    public static final int ROWS = 3;

    public ItemPanel (final byte type, final byte sortBy, final String search, final String tag)
    {
        // setStyleName("inventory_item");
        _type = type;

        _items = new PagedGrid(ROWS, COLUMNS) {
            protected Widget createWidget (Object item) {
                return new ItemContainer((CatalogListing)item, ItemPanel.this);
            }
            protected String getEmptyMessage () {
                return "There are no " + Item.getTypeName(_type) + " items listed.";
            }
        };
        _items.setStyleName("catalogContents");
        add(_items, DockPanel.CENTER);
        add(_status = new Label(""), DockPanel.SOUTH);

        // last of all, initialize the item view with its data model
        _items.setModel(new DataModel() {
            public void doFetchRows (int start, int count, final AsyncCallback callback) {
                setStatus("Loading...");
                CCatalog.catalogsvc.loadCatalog(CCatalog.getMemberId(), type, sortBy, search,
                                                tag, start, count, new AsyncCallback() {
                    public void onSuccess (Object result) {
                        setStatus("");
                        callback.onSuccess(result);
                    }
                    public void onFailure (Throwable caught) {
                        CCatalog.log("loadCatalog failed", caught);
                        setStatus(CCatalog.serverError(caught));
                    }
                });
            }
            public void removeItem (Object item) {
                // currently we do no internal caching, no problem!
            }
        });
    }

    /**
     * Called by the {@link ListingDetailPopup} if the owner requests to delist an item.
     */
    public void itemDelisted (CatalogListing listing)
    {
        _items.removeItem(listing);
    }

    protected void setStatus (String status)
    {
        _status.setText(status);
    }

    protected byte _type;

    protected PagedGrid _items;
    protected Label _status;
}
