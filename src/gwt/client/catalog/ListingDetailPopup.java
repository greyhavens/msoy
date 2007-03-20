//
// $Id$

package client.catalog;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;
import com.threerings.msoy.item.web.CatalogListing;
import com.threerings.msoy.item.web.ItemDetail;

import org.gwtwidgets.client.util.SimpleDateFormat;

import client.item.BaseItemDetailPopup;
import client.util.ClickCallback;
import client.util.ItemUtil;

/**
 * Displays a popup detail view of an item from the catalog.
 */
public class ListingDetailPopup extends BaseItemDetailPopup
{
    public ListingDetailPopup (CatalogListing listing, CatalogPanel panel)
    {
        super(listing.item);
        _listing = listing;
        _panel = panel;

        _listed.setText(CCatalog.msgs.listingListed(_lfmt.format(listing.listedDate)));

        FlexCellFormatter formatter = _price.getFlexCellFormatter();
        formatter.setWidth(0, 0, "25px"); // gap!
        formatter.setStyleName(0, 0, "Icon");
        _price.setWidget(0, 0, new Image("/images/header/symbol_gold.png"));
        _price.setText(0, 1, String.valueOf(_listing.goldCost));

        formatter.setWidth(0, 2, "25px"); // gap!
        formatter.setStyleName(0, 2, "Icon");
        _price.setWidget(0, 2, new Image("/images/header/symbol_flow.png"));
        _price.setText(0, 3, String.valueOf(_listing.flowCost));
    }

    // @Override // BaseItemDetailPopup
    protected void createInterface (VerticalPanel details, VerticalPanel controls)
    {
        super.createInterface(details, controls);

        // we need to create this here so we can pass it to our click callback
        _status = new Label("");

        ItemUtil.addItemSpecificControls(_item, controls, this);

        details.add(_listed = new Label());
        details.add(_price = new FlexTable());

        // TODO: enable/disable purchase button depending on member's gold/flow wealth?
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
        _purchase.setEnabled(CCatalog.getMemberId() > 0);

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
                    CCatalog.catalogsvc.listItem(
                        CCatalog.creds, _item.getIdent(), null, -1, false, this);
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
            _controls.insert(delist, _controls.getWidgetCount()-1);
        }
    }

    protected CatalogListing _listing;
    protected CatalogPanel _panel;

    protected FlexTable _price;
    protected Button _purchase;
    protected Label _listed, _status;

    protected static SimpleDateFormat _lfmt = new SimpleDateFormat("MMM dd, yyyy");
}
