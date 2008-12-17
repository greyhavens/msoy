//
// $Id$

package client.shop;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.gwt.CatalogListing;
import com.threerings.msoy.item.gwt.CatalogService;
import com.threerings.msoy.item.gwt.CatalogServiceAsync;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.comment.CommentsPanel;
import client.item.BaseItemDetailPanel;
import client.item.DoListItemPopup;
import client.item.RemixButton;
import client.item.ShopUtil;
import client.shell.CShell;
import client.shell.DynamicLookup;
import client.shell.ShellMessages;
import client.ui.MsoyUI;
import client.ui.PopupMenu;
import client.util.ClickCallback;
import client.util.Link;
import client.util.ServiceUtil;

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

        HorizontalPanel extras = new HorizontalPanel();
        extras.setStyleName("Extras");

        if (!CShell.isGuest() && isRemixable()) {
            extras.add(new RemixButton(_msgs.listingRemix(),
                Link.createListener(Pages.SHOP, Args.compose(ShopPage.REMIX,
                    _item.getType(), _item.itemId, _listing.catalogId))));
        }

        // create a table to display miscellaneous info and admin/owner actions
        SmartTable info = new SmartTable("Info", 0, 0);
        info.setText(0, 0, _msgs.listingListed(), 1, "What");
        info.setText(0, 1, MsoyUI.formatDate(_listing.listedDate, false));
        info.setText(1, 0, _msgs.listingPurchases(), 1, "What");
        info.setText(1, 1, CatalogListing.PRICING_LIMITED_EDITION == _listing.pricing ?
                _msgs.limitedPurchases(""+_listing.purchases, ""+_listing.salesTarget) :
                ""+_listing.purchases);
        info.setText(2, 0, _msgs.favoritesCount(), 1, "What");
        info.setText(2, 1, "" + _listing.favoriteCount);
        extras.add(info);

        _indeets.add(extras);

        // if we are the creator (lister) of this item, allow us to delist it
        if (_detail.creator.getMemberId() == CShell.getMemberId() || CShell.isSupport()) {
            HorizontalPanel controls = new HorizontalPanel();
            controls.setStyleName("controls");

            Label reprice = new Label(_msgs.listingReprice());
            reprice.addStyleName("actionLabel");
            reprice.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    DoListItemPopup.show(_item, _listing, new DoListItemPopup.ListedListener() {
                        public void itemListed (Item item, boolean updated) {
                            Link.replace(Pages.SHOP, Args.compose(
                                "l", _item.getType(), _listing.catalogId,
                                "repriced_from_" + _listing.quote.getListedAmount()));
                        }
                    });
                }
            });
            controls.add(reprice);

            Label delist = new Label(_msgs.listingDelist());
            new ClickCallback<Void>(delist, _msgs.listingDelistConfirm()) {
                @Override protected boolean callService () {
                    _catalogsvc.removeListing(_item.getType(), _listing.catalogId, this);
                    return true;
                }
                @Override protected boolean gotResult (Void result) {
                    MsoyUI.info(_msgs.msgListingDelisted());
                    _models.itemDelisted(_listing);
                    History.back();
                    return false;
                }
            };
            controls.add(createSeparator());
            controls.add(delist);

            if (_listing.originalItemId != 0) {
                // also add a link to view the original
                String args = Args.compose("d", ""+_item.getType(), ""+_listing.originalItemId);
                controls.add(createSeparator());
                controls.add(Link.create(_msgs.listingViewOrig(), Pages.STUFF, args));
            }

            _details.add(controls);
        }

        // this will contain all of the buy-related interface and will be replaced with the
        // "bought" interface when the buying is done
        _details.add(new BuyPanel(_listing, null));

        // display a comment interface below the listing details
        addTabBelow("Comments", new CommentsPanel(_item.getType(), listing.catalogId, true), true);

//         // if this item supports sub-items, add a tab for those item types
//         byte[] types = _item.getSalableSubTypes();
//         if (types.length > 0) {
//             for (int ii = 0; ii < types.length; ii++) {
//                 addTabBelow(_dmsgs.xlate("pItemType" + types[ii]), new Label("TBD"));
//             }
//         }
    }

    @Override
    protected void addTagMenuItems (final String tag, PopupMenu menu)
    {
        menu.addMenuItem(_cmsgs.tagSearch(), new Command() {
            public void execute() {
                Link.go(Pages.SHOP, ShopUtil.composeArgs(_item.getType(), tag, null, 0));
            }
        });
    }

    protected static Widget createSeparator ()
    {
        return new HTML("&nbsp;&nbsp;|&nbsp;&nbsp;");
    }

    protected CatalogModels _models;
    protected CatalogListing _listing;

    protected static final ShopMessages _msgs = GWT.create(ShopMessages.class);
    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
    protected static final CatalogServiceAsync _catalogsvc = (CatalogServiceAsync)
        ServiceUtil.bind(GWT.create(CatalogService.class), CatalogService.ENTRY_POINT);
}
