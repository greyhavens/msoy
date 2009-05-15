//
// $Id$

package client.shop;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.History;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
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
import client.item.ItemUtil;
import client.item.ConfigButton;
import client.item.ShopUtil;
import client.shell.CShell;
import client.shell.DynamicLookup;
import client.shell.ShellMessages;
import client.ui.MsoyUI;
import client.ui.PopupMenu;
import client.ui.ShareDialog;
import client.util.ClickCallback;
import client.util.Link;
import client.util.InfoCallback;
import client.util.ServiceUtil;

/**
 * Displays a detail view of an item from the catalog.
 */
public class ListingDetailPanel extends BaseItemDetailPanel
{
    public ListingDetailPanel (CatalogModels models, byte type, int catalogId)
    {
        addStyleName("listingDetailPanel");
        _models = models;

        // TODO: display loading swirly

        // ABTEST: 2009 03 buypanel: switched to loadTestedListing
        _catalogsvc.loadTestedListing(
            CShell.frame.getVisitorInfo(), "2009 03 buypanel", type, catalogId,
            new InfoCallback<CatalogService.ListingResult>() {
            public void onSuccess (CatalogService.ListingResult result) {
                gotListing(result.listing, result.abTestGroup);
            }
        });
    }

    @Override
    protected void onLoad ()
    {
        super.onLoad();
        _singleton = this;
        configureBridge();
    }

    @Override
    protected void onUnload ()
    {
        _singleton = null;
        super.onUnload();
    }

    // ABTEST: 2009 03 buypanel: added abTestGroup
    protected void gotListing (CatalogListing listing, int abTestGroup)
    {
        init(listing.detail);
        _listing = listing;

        HorizontalPanel extras = new HorizontalPanel();
        extras.setStyleName("Extras");

        if (!CShell.isGuest() && isRemixable()) {
            extras.add(new ConfigButton(false, _msgs.listingRemix(),
                Link.createHandler(Pages.SHOP, ShopPage.REMIX, _item.getType(), _item.itemId,
                                    _listing.catalogId)));
        }
        extras.add(_configBtn = new ConfigButton(true, _msgs.listingConfig(), new ClickHandler() {
            public void onClick (ClickEvent event) {
                ItemUtil.showViewerConfig();
            }
        }));
        _configBtn.setVisible(false);

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
            reprice.addClickHandler(new ClickHandler() {
                public void onClick (ClickEvent event) {
                    DoListItemPopup.show(_item, _listing, new DoListItemPopup.ListedListener() {
                        public void itemListed (Item item, boolean updated) {
                            Link.replace(Pages.SHOP, "l", _item.getType(), _listing.catalogId,
                                         "repriced_from_" + _listing.quote.getListedAmount());
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
                controls.add(createSeparator());
                controls.add(Link.create(_msgs.listingViewOrig(), Pages.STUFF,
                                         "d", _item.getType(), _listing.originalItemId));
            }

            _details.add(controls);
        }

        // this will contain all of the buy-related interface and will be replaced with the
        // "bought" interface when the buying is done
        // ABTEST: 2009 03 buypanel: added abTestGroup
        _details.add(new ItemBuyPanel(_listing, abTestGroup, null));

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

    @Override // from BaseItemDetailPanel
    protected void addTagMenuItems (final String tag, PopupMenu menu)
    {
        menu.addMenuItem(_cmsgs.tagSearch(), new Command() {
            public void execute() {
                Link.go(Pages.SHOP, ShopUtil.composeArgs(_item.getType(), tag, null, 0));
            }
        });
    }

    @Override // from BaseItemDetailPanel
    protected void addRatableBits (HorizontalPanel row)
    {
        super.addRatableBits(row);

        // we need to create our share info lazily because _listing is null right now as we're
        // being called down to from our superclass constructor
        row.add(MsoyUI.makeShareButton(new ClickHandler() {
            public void onClick (ClickEvent event) {
                ShareDialog.Info info = new ShareDialog.Info();
                info.page = Pages.SHOP;
                info.args = Args.compose("l", _item.getType(), _listing.catalogId);
                info.what = _dmsgs.xlate("itemType" + _item.getType());
                info.title = _item.name;
                info.descrip = _item.description;
                info.image = _item.getThumbnailMedia();
                new ShareDialog(info).show();
           }
        }));
    }

    @Override
    protected boolean inShop ()
    {
        return true;
    }

    protected static Widget createSeparator ()
    {
        return new HTML("&nbsp;&nbsp;|&nbsp;&nbsp;");
    }

    /**
     * A callback from the studio viewer, to indicate that custom config is available.
     */
    protected static void hasCustomConfig ()
    {
        _singleton._configBtn.setVisible(true);
    }

    protected static native void configureBridge () /*-{
        $wnd.hasCustomConfig = function () {
            @client.shop.ListingDetailPanel::hasCustomConfig()();
        };
    }-*/;

    protected CatalogModels _models;
    protected CatalogListing _listing;

    protected ConfigButton _configBtn;

    protected static ListingDetailPanel _singleton;

    protected static final ShopMessages _msgs = GWT.create(ShopMessages.class);
    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
    protected static final CatalogServiceAsync _catalogsvc = (CatalogServiceAsync)
        ServiceUtil.bind(GWT.create(CatalogService.class), CatalogService.ENTRY_POINT);
}
