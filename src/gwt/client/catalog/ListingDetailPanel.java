//
// $Id$

package client.catalog;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import org.gwtwidgets.client.util.SimpleDateFormat;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.item.data.gwt.CatalogListing;
import com.threerings.msoy.item.data.gwt.ItemDetail;

import client.item.BaseItemDetailPanel;
import client.shell.Application;
import client.shell.CommentsPanel;
import client.util.ClickCallback;
import client.util.ItemUtil;
import client.util.MsoyUI;
import client.util.PopupMenu;

/**
 * Displays a detail view of an item from the catalog.
 */
public class ListingDetailPanel extends BaseItemDetailPanel
{
    public ListingDetailPanel (ItemDetail detail, CatalogListing listing, CatalogPanel panel)
    {
        super(detail);
        _listing = listing;
        _panel = panel;

        _listed.setText(CCatalog.msgs.listingListed(_lfmt.format(listing.listedDate)));
        _purchases.setText(CCatalog.msgs.listingPurchases("" + listing.purchases));

        _extras.add(_price = new FlexTable());
        _price.setCellPadding(0);
        _price.setCellSpacing(0);
        FlexCellFormatter formatter = _price.getFlexCellFormatter();
        formatter.setWidth(0, 0, "15px"); // gap!
        formatter.setStyleName(0, 0, "Icon");
        _price.setWidget(0, 0, new Image("/images/header/symbol_gold.png"));
        formatter.setWidth(0, 1, "25px"); // gap!
        _price.setText(0, 1, String.valueOf(_listing.goldCost));

        formatter.setWidth(0, 2, "15px"); // gap!
        formatter.setStyleName(0, 2, "Icon");
        _price.setWidget(0, 2, new Image("/images/header/symbol_flow.png"));
        _price.setText(0, 3, String.valueOf(_listing.flowCost));

        // if we are the creator (lister) of this item, allow us to delist it
        if (_listing.creator.getMemberId() == CCatalog.getMemberId() || CCatalog.isAdmin()) {
            _details.add(WidgetUtil.makeShim(1, 10));
            _details.add(new Label(CCatalog.msgs.listingDelistTip()));
            Button delist = new Button(CCatalog.msgs.listingDelist());
            new ClickCallback(delist) {
                public boolean callService () {
                    CCatalog.catalogsvc.removeListing(
                        CCatalog.ident, _item.getType(), _listing.catalogId, this);
                    return true;
                }
                public boolean gotResult (Object result) {
                    MsoyUI.info(CCatalog.msgs.msgListingDelisted());
                    _panel.itemDelisted(_listing);
                    _panel.showCatalog();
                    return false;
                }
            };
            _details.add(delist);
        }

        // display a comment interface below the listing details
        addTabBelow("Comments", new CommentsPanel(detail.item.getType(), listing.catalogId));

//         // if this item supports sub-items, add a tab for those item types
//         byte[] types = detail.item.getSalableSubTypes();
//         if (types.length > 0) {
//             for (int ii = 0; ii < types.length; ii++) {
//                 addTabBelow(CCatalog.dmsgs.getString("pItemType" + types[ii]), new Label("TBD"));
//             }
//         }
    }

    // @Override // BaseItemDetailPanel
    protected void createInterface (VerticalPanel details)
    {
        super.createInterface(details);

        ItemUtil.addItemSpecificButtons(_item, _buttons);

        // TODO: enable/disable purchase button depending on member's gold/flow wealth?
        _buttons.add(_purchase = new Button(CCatalog.msgs.listingBuy()));
        new ClickCallback(_purchase) {
            public boolean callService () {
                CCatalog.catalogsvc.purchaseItem(
                    CCatalog.ident, _item.getType(), _listing.catalogId, this);
                return true;
            }
            public boolean gotResult (Object result) {
                MsoyUI.info(CCatalog.msgs.msgListingBought());
                return false; // don't reenable purchase
            }
        };
        _purchase.setEnabled(CCatalog.getMemberId() > 0);

        details.add(WidgetUtil.makeShim(1, 10));
        details.add(_listed = new Label());
        details.add(_purchases = new Label());

        _creator.setMember(_detail.creator, new PopupMenu() {
            protected void addMenuItems () {
                this.addMenuItem(CCatalog.imsgs.viewProfile(), new Command() {
                    public void execute () {
                        History.newItem(Application.createLinkToken("profile", 
                            "" + _detail.creator.getMemberId()));
                    }
                });
                this.addMenuItem(CCatalog.imsgs.browseCatalogFor(), new Command() {
                    public void execute () {
                        _panel.browseByCreator(_detail.creator.getMemberId(), 
                            _detail.creator.toString());
                        returnToList();
                    }
                });
            }
        });
    }

    // @Override // BaseItemDetailPanel
    protected void returnToList ()
    {
        _panel.showCatalog();
    }

    protected CatalogListing _listing;
    protected CatalogPanel _panel;

    protected FlexTable _price;
    protected Button _purchase;
    protected Label _purchases, _listed;

    protected static SimpleDateFormat _lfmt = new SimpleDateFormat("MMM dd, yyyy");
}
