//
// $Id$

package client.shop;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SourcesClickEvents;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.Anchor;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.SubItem;
import com.threerings.msoy.item.gwt.CatalogListing;
import com.threerings.msoy.item.gwt.CatalogService;
import com.threerings.msoy.item.gwt.CatalogServiceAsync;
import com.threerings.msoy.item.gwt.CostUpdatedException;
import com.threerings.msoy.money.gwt.InsufficientFundsException;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.PriceQuote;
import com.threerings.msoy.web.gwt.Pages;

import client.item.ItemActivator;
import client.shell.CShell;
import client.shell.DynamicLookup;
import client.ui.MsoyUI;
import client.ui.StretchButton;
import client.util.ClickCallback;
import client.util.FlashClients;
import client.util.Link;
import client.util.MoneyUtil;
import client.util.ServiceUtil;

/**
 * An interface for buying a CatalogListing. Doesn't display anything but functional buy
 * buttons.
 */
public class BuyPanel extends FlowPanel
{
    /**
     * @param callback optional. Notified only on success.
     */
    public BuyPanel (CatalogListing listing, AsyncCallback<Item> callback)
    {
        _listing = listing;
        _callback = callback;
        setStyleName("Buy");

        // Buy with bars, plus a link on how to acquire some
        _buyBars = new BuyButton(Currency.BARS);
        _barPanel = new FlowPanel();
        _barPanel.add(_buyBars);
        _changeLabel = new Label();
        _barPanel.add(_changeLabel);
        Widget link = Link.buyBars(_msgs.listingBuyBars());
        link.setStyleName("GetBars");
        _barPanel.add(link);
        add(_barPanel);

        _buyCoins = new BuyButton(Currency.COINS);
        add(_buyCoins);

        // Display exchange rate 
        _wikiLink = MsoyUI.createExternalAnchor("http://wiki.whirled.com/Currency", "");
        
        updatePrice(_listing.quote);
    }

    protected void itemPurchased (CatalogService.PurchaseResult result, Currency currency)
    {
        Item item = result.item;
        byte itype = item.getType();

        // clear out the buy button
        clear();
        setStyleName("Bought");

        // change the buy button into a "you bought it" display
        String type = _dmsgs.xlate("itemType" + itype);
        add(MsoyUI.createLabel(_msgs.boughtTitle(type), "Title"));

        if (FlashClients.clientExists()) {
            if (item instanceof SubItem) {
                add(WidgetUtil.makeShim(10, 10));
                add(MsoyUI.createButton(MsoyUI.LONG_THIN, _msgs.boughtBackTo(), new ClickListener() {
                    public void onClick (Widget sender) {
                        CShell.frame.closeContent();
                    }
                }));
            } else {
                add(new ItemActivator(item, true));
                add(new Label(getUsageMessage(itype)));
            }

        } else {
            add(new Label(_msgs.boughtViewStuff(type)));
            String ptype = _dmsgs.xlate("pItemType" + itype);
            add(Link.create(_msgs.boughtGoNow(ptype), Pages.STUFF, ""+itype));
        }

        if (result.charity != null) {
            String percentage = NumberFormat.getPercentFormat().format(
                (int)(100.0 * result.charityPercentage) / 100.0);
            add(WidgetUtil.makeShim(10, 10));
            FlowPanel charityPanel = new FlowPanel();
            charityPanel.setStyleName("Charity");
            charityPanel.add(new InlineLabel(_msgs.donatedToCharity(percentage)));
            charityPanel.add(Link.create(result.charity.toString(),
                                         Pages.PEOPLE, ""+result.charity.getMemberId()));
            charityPanel.add(WidgetUtil.makeShim(10, 10));
            charityPanel.add(Link.create(_msgs.changeCharity(), Pages.ACCOUNT, "edit"));
            add(charityPanel);
        }
        
        FlowPanel again = new FlowPanel();
        again.setStyleName("Buy");
        BuyButton buyAgain = (currency == Currency.BARS) ? _buyBars : _buyCoins;
        buyAgain.addStyleDependentName("small");
        again.add(buyAgain);
        again.add(MsoyUI.createLabel(_msgs.timesBought(""+_timesBought), null));
        add(again);

        if (_callback != null) {
            _callback.onSuccess(item);
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

    protected void updatePrice (PriceQuote quote)
    {
        _listing.quote = quote;

        _barPanel.setVisible(quote.getCoins() > 0);
        _buyBars.setAmount(quote.getBars());
        _changeLabel.setText(quote.getCoinChange() > 0 ? 
            _msgs.listingCoinChange(Currency.COINS.format(
            _listing.quote.getCoinChange())) : "");
        _buyCoins.setAmount(quote.getCoins());
        if (quote.getListedCurrency() == Currency.BARS) {
            _wikiLink.setText(_msgs.listingExchangeRate(Currency.COINS.format(
                (int)quote.getExchangeRate())));
            add(_wikiLink);
        } else {
            remove(_wikiLink);
        }
    }

    protected class BuyCallback extends ClickCallback<CatalogService.PurchaseResult>
    {
        public BuyCallback (SourcesClickEvents button, Currency currency)
        {
            super(button);
            _currency = currency;
        }

        @Override protected boolean callService ()
        {
            if (CShell.isGuest()) {
                MsoyUI.infoAction(_msgs.msgMustRegister(), _msgs.msgRegister(),
                                  Link.createListener(Pages.ACCOUNT, "create"));
            } else {
                _catalogsvc.purchaseItem(_listing.detail.item.getType(), _listing.catalogId,
                                         _currency, _listing.quote.getAmount(_currency), this);
            }
            return true;
        }

        @Override protected boolean gotResult (CatalogService.PurchaseResult result)
        {
            _timesBought += 1;
            MoneyUtil.updateBalances(result.balances);
            updatePrice(result.quote);
            itemPurchased(result, _currency);
            return true;
        }

        public void onFailure (Throwable cause)
        {
            super.onFailure(cause);

            if (cause instanceof CostUpdatedException) {
                CostUpdatedException cue = (CostUpdatedException) cause;
                updatePrice(cue.getQuote());

            } else if (cause instanceof InsufficientFundsException) {
                MoneyUtil.updateBalances(((InsufficientFundsException) cause).getBalances());
            }
        }

        protected Currency _currency;
    }; // end: class BuyCallback


    protected class BuyButton extends StretchButton
    {
        public BuyButton (Currency currency)
        {
            super(currency == Currency.BARS ? ORANGE_THICK : BLUE_THICK, null);
            _currency = currency;
            addStyleName("buyButton");
            new BuyCallback(this, currency);
        }

        public void setAmount (int amount)
        {
            HorizontalPanel horiz = new HorizontalPanel();
            horiz.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);

            horiz.add(MsoyUI.createLabel((amount > 0) ? _msgs.shopBuy() : _msgs.shopFree(), null));
            horiz.add(WidgetUtil.makeShim(10, 1));
            horiz.add(MsoyUI.createImage(_currency.getLargeIcon(), null));
            horiz.add(WidgetUtil.makeShim(10, 1));
            horiz.add(MsoyUI.createLabel(_currency.format(amount), null));

            setContent(horiz);
        }

        protected Currency _currency;
    }; // end: class BuyButton

    protected CatalogListing _listing;

    protected AsyncCallback<Item> _callback;

    protected BuyButton _buyBars, _buyCoins;
    protected FlowPanel _barPanel;
    protected Label _changeLabel;
    protected Anchor _wikiLink;
    
    protected int _timesBought;

    protected static final ShopMessages _msgs = GWT.create(ShopMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
    protected static final CatalogServiceAsync _catalogsvc = (CatalogServiceAsync)
        ServiceUtil.bind(GWT.create(CatalogService.class), CatalogService.ENTRY_POINT);
}
