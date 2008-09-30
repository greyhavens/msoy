//
// $Id$

package client.shop;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SourcesClickEvents;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.SubItem;
import com.threerings.msoy.item.gwt.CatalogListing;
import com.threerings.msoy.item.gwt.CatalogService;
import com.threerings.msoy.item.gwt.CatalogServiceAsync;
import com.threerings.msoy.item.gwt.CostUpdatedException;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.PriceQuote;

import client.comment.CommentsPanel;
import client.item.BaseItemDetailPanel;
import client.item.DoListItemPopup;
import client.item.ItemActivator;
import client.item.RemixButton;
import client.item.ShopUtil;
import client.shell.Args;
import client.shell.DynamicLookup;
import client.shell.Pages;
import client.shell.ShellMessages;
import client.ui.MsoyUI;
import client.ui.PopupMenu;
import client.ui.StretchButton;
import client.util.ClickCallback;
import client.util.FlashClients;
import client.util.Link;
import client.util.NaviUtil;
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

        if (!CShop.isGuest() && isRemixable()) {
            _indeets.add(WidgetUtil.makeShim(10, 10));
            _indeets.add(new RemixButton(_msgs.listingRemix(),
                // TODO: Bar me
                NaviUtil.onRemixCatalogItem(_item.getType(), _item.itemId, _listing.catalogId,
                    _listing.quote.getCoins(), _listing.quote.getBars())));
        }

//         _indeets.add(WidgetUtil.makeShim(10, 10));
//         Currency listedCur = _listing.quote.getListedCurrency();
//         _indeets.add(_priceLabel = new PriceLabel(listedCur, _listing.quote.getListedAmount()));

        // create a table to display miscellaneous info and admin/owner actions
        //info.setStyle("Info"); // ?
//        info.setText(0, 0, _msgs.listingListed(), 1, "What");
//        info.setText(0, 1, MsoyUI.formatDate(listing.listedDate));
//        info.setText(1, 0, _msgs.listingPurchases(), 1, "What");
//        info.setText(1, 1, "" + listing.purchases);
//        info.setText(2, 0, _msgs.favoritesCount(), 1, "What");
//        info.setText(2, 1, "" + listing.favoriteCount);

        // if we are the creator (lister) of this item, allow us to delist it
        if (_detail.creator.getMemberId() == CShop.getMemberId() || CShop.isSupport()) {
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
                public boolean callService () {
                    _catalogsvc.removeListing(_item.getType(), _listing.catalogId, this);
                    return true;
                }
                public boolean gotResult (Void result) {
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
        _buyPanel = new FlowPanel();
        _buyPanel.setStyleName("Buy");

        // Buy with bars, plus a link on how to acquire some
        _buyBars = new BuyButton(Currency.BARS, _listing.quote.getBars());
        if (DeploymentConfig.barsEnabled) {
            _buyPanel.add(_buyBars);
            Widget link = Link.buyBars(_msgs.listingBuyBars());
            link.setStyleName("GetBars");
            _buyPanel.add(link);
        }

        _buyCoins = new BuyButton(Currency.COINS, _listing.quote.getCoins());
        _buyPanel.add(_buyCoins);
        _details.add(_buyPanel);

        String when = MsoyUI.formatDate(_listing.listedDate, false);
        _details.add(MsoyUI.createLabel(_msgs.listedOn(when), "listedDate"));

        // display a comment interface below the listing details
        addTabBelow("Comments", new CommentsPanel(_item.getType(), listing.catalogId), true);

//         // if this item supports sub-items, add a tab for those item types
//         byte[] types = _item.getSalableSubTypes();
//         if (types.length > 0) {
//             for (int ii = 0; ii < types.length; ii++) {
//                 addTabBelow(_dmsgs.xlate("pItemType" + types[ii]), new Label("TBD"));
//             }
//         }
    }

    protected void itemPurchased (Item item)
    {
        byte itype = item.getType();

        // clear out the buy button
        _buyPanel.clear();
        _buyPanel.setStyleName("Bought");

        // change the buy button into a "you bought it" display
        String type = _dmsgs.xlate("itemType" + itype);
        _buyPanel.add(MsoyUI.createLabel(_msgs.boughtTitle(type), "Title"));

        if (FlashClients.clientExists() && !(item instanceof SubItem)) {
            _buyPanel.add(new ItemActivator(item, true));
            _buyPanel.add(new Label(getUsageMessage(itype)));
        } else {
            _buyPanel.add(new Label(_msgs.boughtViewStuff(type)));
            String ptype = _dmsgs.xlate("pItemType" + itype);
            _buyPanel.add(Link.create(_msgs.boughtGoNow(ptype), Pages.STUFF, ""+itype));
        }
    }

    protected static String getUsageMessage (byte itemType)
    {
        if (itemType == Item.AVATAR) {
            return _msgs.boughtAvatarUsage();
        } else if (itemType == Item.DECOR) {
            return _msgs.boughtDecorUsage();
        } else if (itemType == Item.AUDIO) {
            return _msgs.boughtAudioUsage();
        } else if (itemType == Item.PET) {
            return _msgs.boughtPetUsage();
        } else {
            return _msgs.boughtOtherUsage();
        }
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

    protected void updatePrice (PriceQuote quote)
    {
        _buyBars.setAmount(quote.getBars());
        _buyCoins.setAmount(quote.getCoins());
    }

    protected class BuyCallback extends ClickCallback<Item>
    {
        public BuyCallback (SourcesClickEvents button, Currency currency)
        {
            super(button);
            _currency = currency;
        }

        public boolean callService ()
        {
            if (CShop.isGuest()) {
                MsoyUI.infoAction(_msgs.msgMustRegister(), _msgs.msgRegister(),
                                  Link.createListener(Pages.ACCOUNT, "create"));
            } else {
                _catalogsvc.purchaseItem(_item.getType(), _listing.catalogId,
                                         _currency, _listing.quote.getAmount(_currency), this);
            }
            return true;
        }

        public boolean gotResult (Item item)
        {
            itemPurchased(item);
            return false; // don't reenable buy button
        }

        public void onFailure (Throwable cause)
        {
            super.onFailure(cause);

            if (cause instanceof CostUpdatedException) {
                updatePrice( ((CostUpdatedException)cause).getQuote());
            }
        }

        protected Currency _currency;
    };

    protected class BuyButton extends StretchButton
    {
        public BuyButton (Currency currency, int amount)
        {
            super(currency == Currency.BARS ? "orangeThick" : "blueThick", null);
            _currency = currency;
            addStyleName("buyButton");
            new BuyCallback(this, _currency);
            setAmount(amount);
        }

        public void setAmount (int amount)
        {
            HorizontalPanel horiz = new HorizontalPanel();
            horiz.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
            horiz.add(MsoyUI.createLabel(_msgs.shopBuy(), null));
            horiz.add(WidgetUtil.makeShim(10, 1));
            horiz.add(MsoyUI.createImage(_currency.getLargeIcon(), null));
            horiz.add(WidgetUtil.makeShim(10, 1));
            horiz.add(MsoyUI.createLabel(_currency.format(amount), null));

            setContent(horiz);
        }

        protected Currency _currency;
    };

    protected static Widget createSeparator ()
    {
        return new HTML("&nbsp;&nbsp;|&nbsp;&nbsp;");
    }

    protected CatalogModels _models;
    protected CatalogListing _listing;
    protected FlowPanel _buyPanel;
    protected BuyButton _buyBars, _buyCoins;

    protected static final ShopMessages _msgs = GWT.create(ShopMessages.class);
    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
    protected static final CatalogServiceAsync _catalogsvc = (CatalogServiceAsync)
        ServiceUtil.bind(GWT.create(CatalogService.class), CatalogService.ENTRY_POINT);
}
