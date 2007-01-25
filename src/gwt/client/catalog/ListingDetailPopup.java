//
// $Id$

package client.catalog;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.threerings.msoy.item.web.CatalogListing;
import com.threerings.msoy.item.web.ItemDetail;

import client.item.BaseItemDetailPopup;
import client.util.ClickCallback;

/**
 * Displays a popup detail view of an item from the catalog.
 */
public class ListingDetailPopup extends BaseItemDetailPopup
{
    public ListingDetailPopup (CatalogListing listing, ItemPanel panel)
    {
        super(listing.item);
        _listing = listing;
        _panel = panel;
    }

    // @Override // BaseItemDetailPopup
    protected void createInterface (VerticalPanel details, VerticalPanel controls)
    {
        super.createInterface(details, controls);

        // we need to create this here so we can pass it to our click callback
        _status = new Label("");

        // TODO: add cost

        controls.add(_purchase = new Button(CCatalog.msgs.listingBuy()));
        new ClickCallback(_purchase, _status) {
            public boolean callService () {
                CCatalog.catalogsvc.purchaseItem(CCatalog.creds, _item.getIdent(), this);
                return true;
            }
            public boolean gotResult (Object result) {
                _status.setText(CCatalog.msgs.msgListingBought());
                return false; // don't reenable purchase
            }
        };

        controls.add(_status);
    }

    // @Override // from BaseItemDetailPopup
    protected void gotDetail (ItemDetail detail)
    {
        super.gotDetail(detail);

        // if we are the creator (lister) of this item, allow us to delist it
        if (_listing.creator.getMemberId() == CCatalog.getMemberId()) {
            Button delist = new Button(CCatalog.msgs.listingDelist());
            new ClickCallback(delist, _status) {
                public boolean callService () {
                    CCatalog.catalogsvc.listItem(CCatalog.creds, _item.getIdent(), false, this);
                    return true;
                }
                public boolean gotResult (Object result) {
                    if (result != null) {
                        _status.setText(CCatalog.msgs.msgListingDelisted());
                        _panel.itemDelisted(_listing);
                        return false; // don't reenable delist
                    } else {
                        _status.setText(CCatalog.msgs.errListingNotFound());
                        return true;
                    }
                }
            };
            _controls.insert(delist, 0);
        }
    }

    protected CatalogListing _listing;
    protected ItemPanel _panel;

    protected Button _purchase;
    protected Label _status;
}
