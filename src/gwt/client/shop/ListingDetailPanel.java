//
// $Id$

package client.shop;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.gwtwidgets.client.util.SimpleDateFormat;

import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.gwt.CatalogListing;
import com.threerings.msoy.item.data.gwt.ItemDetail;

import client.item.BaseItemDetailPanel;
import client.shell.Application;
import client.shell.Args;
import client.shell.CommentsPanel;
import client.shell.Frame;
import client.shell.Page;
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

        // createInterface() has already been called by our superclass constructor so now we can
        // fill in information from our listing record
        _listed.setText(CShop.msgs.listingListed(_lfmt.format(listing.listedDate)));
        _purchases.setText(CShop.msgs.listingPurchases("" + listing.purchases));
//         _goldCost.setText(String.valueOf(_listing.goldCost));
        _flowCost.setText(String.valueOf(_listing.flowCost));

        // if we are the creator (lister) of this item, allow us to delist it
        if (_listing.creator.getMemberId() == CShop.getMemberId() || CShop.isAdmin()) {
            Button delist = new Button(CShop.msgs.listingDelist());
            new ClickCallback(delist, CShop.msgs.listingDelistConfirm()) {
                public boolean callService () {
                    CShop.catalogsvc.removeListing(
                        CShop.ident, _item.getType(), _listing.catalogId, this);
                    return true;
                }
                public boolean gotResult (Object result) {
                    MsoyUI.info(CShop.msgs.msgListingDelisted());
                    _panel.itemDelisted(_listing);
                    History.back();
                    return false;
                }
            };
            _buttons.add(delist);

            if (_listing.originalItemId != 0) {
                // also add a link to view the original
                _details.add(Application.createLink(
                                 CShop.msgs.listingViewOrig(), Page.STUFF,
                                 Args.compose(""+detail.item.getType(), "0",
                                              ""+_listing.originalItemId)));
            }
        }

        // display a comment interface below the listing details
        addTabBelow("Comments", new CommentsPanel(detail.item.getType(), listing.catalogId), true);

//         // if this item supports sub-items, add a tab for those item types
//         byte[] types = detail.item.getSalableSubTypes();
//         if (types.length > 0) {
//             for (int ii = 0; ii < types.length; ii++) {
//                 addTabBelow(CShop.dmsgs.getString("pItemType" + types[ii]), new Label("TBD"));
//             }
//         }
    }

    // @Override // BaseItemDetailPanel
    protected void createInterface (VerticalPanel details)
    {
        super.createInterface(details);

        ItemUtil.addItemSpecificButtons(_item, _buttons);

        details.add(WidgetUtil.makeShim(1, 10));
        FlowPanel price = new FlowPanel();
        price.add(new InlineLabel(CShop.msgs.listingPrice(), false, false, true));
//         price.add(MsoyUI.createInlineImage("/images/header/symbol_gold.png"));
//         price.add(_goldCost = new InlineLabel("", false, false, true));
        price.add(MsoyUI.createInlineImage("/images/header/symbol_flow.png"));
        price.add(_flowCost = new InlineLabel("", false, false, true));
        details.add(price);

        details.add(_purchase = new Button(CShop.msgs.listingBuy()));
        _purchase.addStyleName("bigButton"); // make it big!
        _purchase.addStyleName("buyButton"); // really big!
        new ClickCallback(_purchase) {
            public boolean callService () {
                CShop.catalogsvc.purchaseItem(
                    CShop.ident, _item.getType(), _listing.catalogId, this);
                return true;
            }
            public boolean gotResult (Object result) {
                // tell our parent panel that we bought the item, it can display fanciness
                _panel.itemPurchased((Item)result);
                return false; // don't reenable buy button
            }
        };
        _purchase.setEnabled(CShop.getMemberId() > 0);

        details.add(WidgetUtil.makeShim(1, 10));
        details.add(_listed = new Label());
        details.add(_purchases = new Label());

        _creator.setMember(_detail.creator, new PopupMenu() {
            protected void addMenuItems () {
                this.addMenuItem(CShop.imsgs.viewProfile(), new Command() {
                    public void execute () {
                        Application.go(Page.PEOPLE, "" + _detail.creator.getMemberId());
                    }
                });
                this.addMenuItem(CShop.imsgs.browseCatalogFor(), new Command() {
                    public void execute () {
                        _panel.browseByCreator(
                            _detail.creator.getMemberId(), _detail.creator.toString());
                    }
                });
            }
        });
    }

    protected CatalogListing _listing;
    protected CatalogPanel _panel;

    protected Button _purchase;
    protected Label _purchases, _listed;
    protected Label _goldCost, _flowCost;

    protected static SimpleDateFormat _lfmt = new SimpleDateFormat("MMM dd, yyyy");
}
