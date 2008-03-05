//
// $Id$

package client.shop;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;
import org.gwtwidgets.client.util.SimpleDateFormat;

import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.gwt.CatalogListing;
import com.threerings.msoy.item.data.gwt.ItemDetail;

import client.item.BaseItemDetailPanel;
import client.shell.Application;
import client.shell.Args;
import client.shell.CommentsPanel;
import client.shell.Page;
import client.util.ClickCallback;
import client.util.FlashClients;
import client.util.ItemUtil;
import client.util.MsoyUI;

/**
 * Displays a detail view of an item from the catalog.
 */
public class ListingDetailPanel extends BaseItemDetailPanel
{
    public ListingDetailPanel (CatalogModels models, CatalogListing listing)
    {
        super(listing.detail);
        addStyleName("listingDetailPanel");

        _models = models;
        _listing = listing;

// TODO
//         ItemUtil.addItemSpecificButtons(_item, _buttons);

        _indeets.add(WidgetUtil.makeShim(10, 10));
        _indeets.add(new PriceLabel(_listing.flowCost, _listing.goldCost));

        _details.add(WidgetUtil.makeShim(10, 10));
        PushButton purchase = MsoyUI.createButton(MsoyUI.SHORT_THICK, CShop.msgs.listingBuy(), null);
        new ClickCallback(purchase) {
            public boolean callService () {
                CShop.catalogsvc.purchaseItem(
                    CShop.ident, _item.getType(), _listing.catalogId, this);
                return true;
            }
            public boolean gotResult (Object result) {
                itemPurchased((Item)result);
                return false; // don't reenable buy button
            }
        };
        purchase.setEnabled(CShop.getMemberId() > 0);
        _details.add(purchase);

        // create a table to display miscellaneous info and admin/owner actions
        SmartTable info = new SmartTable("Info", 0, 5);
        info.setText(0, 0, CShop.msgs.listingListed(), 1, "What");
        info.setText(0, 1, _lfmt.format(listing.listedDate));
        info.setText(1, 0, CShop.msgs.listingPurchases(), 1, "What");
        info.setText(1, 1, "" + listing.purchases);

        // if we are the creator (lister) of this item, allow us to delist it
        if (_detail.creator.getMemberId() == CShop.getMemberId() || CShop.isAdmin()) {
            Label delist = new Label(CShop.msgs.listingDelist());
            new ClickCallback(delist, CShop.msgs.listingDelistConfirm()) {
                public boolean callService () {
                    CShop.catalogsvc.removeListing(
                        CShop.ident, _item.getType(), _listing.catalogId, this);
                    return true;
                }
                public boolean gotResult (Object result) {
                    MsoyUI.info(CShop.msgs.msgListingDelisted());
                    _models.itemDelisted(_listing);
                    History.back();
                    return false;
                }
            };
            info.addWidget(delist, 2, null);

            if (_listing.originalItemId != 0) {
                // also add a link to view the original
                String args = Args.compose(""+_item.getType(), "0", ""+_listing.originalItemId);
                info.addWidget(Application.createLink(CShop.msgs.listingViewOrig(),
                                                      Page.STUFF, args), 2, null);
            }
        }

        _details.add(WidgetUtil.makeShim(10, 10));
        _details.add(info);

        // display a comment interface below the listing details
        addTabBelow("Comments", new CommentsPanel(_item.getType(), listing.catalogId), true);

//         // if this item supports sub-items, add a tab for those item types
//         byte[] types = _item.getSalableSubTypes();
//         if (types.length > 0) {
//             for (int ii = 0; ii < types.length; ii++) {
//                 addTabBelow(CShop.dmsgs.getString("pItemType" + types[ii]), new Label("TBD"));
//             }
//         }
    }

    protected void itemPurchased (Item item)
    {
        // report to the client that we generated a tutorial event
        if (item.getType() == Item.DECOR) {
            FlashClients.tutorialEvent("decorBought");
        } else if (item.getType() == Item.FURNITURE) {
            FlashClients.tutorialEvent("furniBought");
        } else if (item.getType() == Item.AVATAR) {
            FlashClients.tutorialEvent("avatarBought");
        }

        // display the "you bought an item" UI
        // TODO: setWidget(new BoughtItemPanel(item));
    }

    protected CatalogModels _models;
    protected CatalogListing _listing;

    protected static SimpleDateFormat _lfmt = new SimpleDateFormat("MMM dd, yyyy");
}
