//
// $Id: BuyPanel.java 14685 2009-02-05 02:41:10Z ray $

package client.money;

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

import com.threerings.msoy.money.gwt.CostUpdatedException;
import com.threerings.msoy.money.gwt.InsufficientFundsException;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.PriceQuote;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.PurchaseResult;

import client.ui.MsoyUI;
import client.ui.StretchButton;
import client.util.ClickCallback;
import client.util.MoneyUtil;
import client.util.Link;

/**
 * A base-class interface for buying an item in whirled.
 */
public abstract class BuyPanel<T extends PurchaseResult> extends FlowPanel
{
    /**
     */
    public BuyPanel (PriceQuote quote)
    {
        setStyleName("Buy");

        // Buy with bars, plus a link on how to acquire some
        _buyBars = new BuyButton(Currency.BARS);
        _barPanel = new FlowPanel();
        _barPanel.add(_buyBars);
        _changeLabel = new Label();
        _barPanel.add(_changeLabel);
        Widget link = Link.buyBars(_msgs.getBars());
        link.setStyleName("GetBars");
        _barPanel.add(link);
        add(_barPanel);

        _buyCoins = new BuyButton(Currency.COINS);
        add(_buyCoins);

        // Display exchange rate 
        _wikiLink = MsoyUI.createExternalAnchor("http://wiki.whirled.com/Currency", "");
        
        updatePrice(quote);
    }

    /**
     * Do the service request to make the purchase.
     */
    protected abstract void makePurchase (
        Currency currency, int amount, AsyncCallback<T> listener);

    /**
     * Add any special UI to the BuyPanel after a purchase has executed.
     */
    protected void addPurchasedUI (T result, Currency currency)
    {
        // nothing by default
    }

    /**
     * Handle the result of the purchase.
     */
    protected void wasPurchased (T result, Currency currency)
    {
        // clear out the buy button
        clear();
        setStyleName("Bought");

        addPurchasedUI(result, currency);

        if (result.charity != null) {
            String percentage = NumberFormat.getPercentFormat().format(
                (int)(100.0 * result.charityPercentage) / 100.0);
            add(WidgetUtil.makeShim(10, 10));
            FlowPanel charityPanel = new FlowPanel();
            charityPanel.setStyleName("Charity");
            charityPanel.add(new InlineLabel(_msgs.donatedToCharity(percentage)));
            charityPanel.add(Link.create(
                result.charity.toString(), Pages.PEOPLE, ""+result.charity.getMemberId()));
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
    }

    protected void updatePrice (PriceQuote quote)
    {
        _quote = quote;

        _barPanel.setVisible(quote.getCoins() > 0);
        _buyBars.setAmount(quote.getBars());
        _changeLabel.setText(quote.getCoinChange() > 0 ?
            _msgs.coinChange(Currency.COINS.format(quote.getCoinChange())) : "");
        _buyCoins.setAmount(quote.getCoins());
        if (quote.getListedCurrency() == Currency.BARS) {
            _wikiLink.setText(_msgs.exchangeRate(Currency.COINS.format(
                (int)Math.ceil(quote.getExchangeRate()))));
            add(_wikiLink);
        } else {
            remove(_wikiLink);
        }
    }

    protected class BuyCallback extends ClickCallback<T>
    {
        public BuyCallback (SourcesClickEvents button, Currency currency)
        {
            super(button);
            _currency = currency;
        }

        @Override protected boolean callService ()
        {
            makePurchase(_currency, _quote.getAmount(_currency), this);
            return true;
        }

        @Override protected boolean gotResult (T result)
        {
            _timesBought++;
            MoneyUtil.updateBalances(result.balances);
            updatePrice(result.quote);
            wasPurchased(result, _currency);
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

            horiz.add(MsoyUI.createLabel((amount > 0) ? _msgs.buy() : _msgs.buyFree(), null));
            horiz.add(WidgetUtil.makeShim(10, 1));
            horiz.add(MsoyUI.createImage(_currency.getLargeIcon(), null));
            horiz.add(WidgetUtil.makeShim(10, 1));
            horiz.add(MsoyUI.createLabel(_currency.format(amount), null));

            setContent(horiz);
        }

        protected Currency _currency;
    }; // end: class BuyButton

    protected PriceQuote _quote;

    protected BuyButton _buyBars, _buyCoins;
    protected FlowPanel _barPanel;
    protected Label _changeLabel;
    protected Anchor _wikiLink;
    
    protected int _timesBought;

    protected static final MoneyMessages _msgs = GWT.create(MoneyMessages.class);
}
