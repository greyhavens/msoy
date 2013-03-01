//
// $Id$

package client.money;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.PriceQuote;
import com.threerings.msoy.money.data.all.PurchaseResult;
import com.threerings.msoy.money.gwt.CostUpdatedException;
import com.threerings.msoy.money.gwt.InsufficientFundsException;
import com.threerings.msoy.web.gwt.Pages;

import client.ui.MsoyUI;
import client.ui.RoundBox;
import client.ui.StretchButton;
import client.util.ClickCallback;
import client.util.Link;
import client.util.MoneyUtil;

/**
 * A base-class interface for buying an item in whirled.
 */
public abstract class BuyPanel<T> extends SmartTable
{
    /**
     * Return a generic widget hosting this BuyPanel with the specified message.
     */
    public RoundBox createPromptHost (String promptStr)
    {
        RoundBox box = new RoundBox(RoundBox.BLUE);
        box.setHorizontalAlignment(HasAlignment.ALIGN_CENTER);
        box.add(MsoyUI.createLabel(promptStr, "buyPrompt"));
        box.add(this);
        return box;
    }

    /**
     * Initialize the buy panel. (Separted from constructor to reduce subclassing pain.)
     *
     * @param quote the price quote
     * @param callback optional, notified only on success.
     */
    public void init (PriceQuote quote, AsyncCallback<T> callback)
    {
        _callback = callback;
        setStyleName("buyPanel");

        /** A handler to switch the displayed currency. */
        ClickHandler switchCurrency = new ClickHandler() {
            public void onClick (ClickEvent event) {
                _altCurrency = !_altCurrency;
                updatePrice(_quote); // rejigger the UI
            }
        };

        // Buy with bars, plus a link on how to acquire some
        _barPanel = new FlowPanel();
        _barPanel.add(_buyBars = new BuyButton(Currency.BARS));
        _barPanel.add(_barLabel = MsoyUI.createHTML("", "inline"));
        _barPanel.add(_switchToCoins = MsoyUI.createActionLabel(
                          _msgs.buyWithCoins(), "inline", switchCurrency));
        setWidget(0, 0, _barPanel, 2);
        getFlexCellFormatter().setHorizontalAlignment(0, 0, HasAlignment.ALIGN_CENTER);

        // Buy with coins, plus a link to switch to buying with bars
        _coinPanel = new FlowPanel();
        _coinPanel.add(_buyCoins = new BuyButton(Currency.COINS));
        setWidget(1, 0, _coinPanel, 2);
        getFlexCellFormatter().setHorizontalAlignment(1, 0, HasAlignment.ALIGN_CENTER);

        updatePrice(quote);
    }

    /**
     * Do the service request to make the purchase.
     * @return true if a service request was sent.
     */
    protected abstract boolean makePurchase (
        Currency currency, int amount, AsyncCallback<PurchaseResult<T>> listener);

    /**
     * Add any special UI to the BuyPanel after a purchase has executed.
     * @param boughtPanel
     */
    protected void addPurchasedUI (T ware, FlowPanel boughtPanel)
    {
        // nothing by default
    }

    /**
     * Handle the result of the purchase.
     */
    protected void wasPurchased (PurchaseResult<T> result, Currency currency)
    {
        // clear out the buy button
        clear();
        setStyleName("boughtPanel");

        FlowPanel boughtPanel = new FlowPanel();
        addPurchasedUI(result.ware, boughtPanel);

        if (result.charity != null) {
            String percentage = NumberFormat.getPercentFormat().format(
                (int)(100.0 * result.charityPercentage) / 100.0);
            boughtPanel.add(WidgetUtil.makeShim(10, 10));
            FlowPanel charityPanel = new FlowPanel();
            charityPanel.setStyleName("Charity");
            charityPanel.add(MsoyUI.createLabel(_msgs.donatedToCharity(percentage), "inline"));
            charityPanel.add(Link.create(
                result.charity.toString(), Pages.PEOPLE, ""+result.charity.getId()));
            charityPanel.add(WidgetUtil.makeShim(10, 10));
            charityPanel.add(Link.create(_msgs.changeCharity(), Pages.ACCOUNT, "edit"));
            boughtPanel.add(charityPanel);
        }

        FlowPanel again = new FlowPanel();
        again.setStyleName("Buy");
        BuyButton buyAgain = (currency == Currency.BARS) ? _buyBars : _buyCoins;
        buyAgain.addStyleDependentName("small");
        again.add(buyAgain);
        again.add(MsoyUI.createLabel(_msgs.timesBought(""+_timesBought), null));
        boughtPanel.add(again);

        setWidget(0, 0, boughtPanel);
    }

    protected void updatePrice (PriceQuote quote)
    {
        _quote = quote;

        boolean barOnly = (quote.getCoins() < 0); //(quote.getListedCurrency() == Currency.BARS)
        _switchToCoins.setVisible(!barOnly);

        _buyBars.setAmount(quote.getBars());
        _buyCoins.setAmount(quote.getCoins());

        String barTip;
        if (barOnly) {
            // this is a BAR-ONLY item
            _coinPanel.setVisible(false);
            _barPanel.setVisible(true);
            int cents = quote.getCentsPerBar() * quote.getBars();
            barTip = (cents == 0) ? "" : (cents < 100) ? _msgs.centsCost("" + cents)
                : _msgs.dollarCost(NumberFormat.getFormat("$0.00").format(cents / 100f));
        } else {
            // the item is priced in coins, so either one will work
            _coinPanel.setVisible(!_altCurrency);
            _barPanel.setVisible(_altCurrency);
            int change = quote.getCoinChange();
            barTip = (change == 0) ? "" : _msgs.coinChange(Currency.COINS.format(change));
        }
        _barLabel.setHTML(barTip + "&nbsp;"); // mind the gap
    }

    protected class BuyCallback extends ClickCallback<PurchaseResult<T>>
    {
        public BuyCallback (HasClickHandlers button, Currency currency) {
            super(button);
            _currency = currency;
        }

        @Override protected boolean callService () {
            return makePurchase(_currency, _quote.getAmount(_currency), this);
        }

        @Override protected boolean gotResult (PurchaseResult<T> result) {
            _timesBought++;
            MoneyUtil.updateBalances(result.balances);
            wasPurchased(result, _currency);
            if (_callback != null) {
                _callback.onSuccess(result.ware);
            }
            return true;
        }

        @Override public void onFailure (Throwable cause) {
            super.onFailure(cause);

            if (cause instanceof CostUpdatedException) {
                CostUpdatedException cue = (CostUpdatedException) cause;
                updatePrice(cue.getQuote());

            } else if (cause instanceof InsufficientFundsException) {
                MoneyUtil.updateBalances(((InsufficientFundsException) cause).getBalances());
            }
        }

        @Override protected void reportFailure (Throwable cause) {
            // if we have NSF, we want a custom error message
            String msg = null;
            if (cause instanceof InsufficientFundsException) {
                switch (((InsufficientFundsException)cause).getCurrency()) {
                case COINS:
                    msg = _msgs.insufficientCoins();
                    break;
                case BARS:
                    msg = _msgs.insufficientBars();
                    break;
                }
            }
            if (msg == null) {
                super.reportFailure(cause);
            } else {
                MsoyUI.info(msg);
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
            addStyleName("BuyButton");
            new BuyCallback(this, currency);
        }

        public void setAmount (int amount)
        {
            // hide us if the amount is invalid
            setVisible(amount >= 0);

            HorizontalPanel horiz = new HorizontalPanel();
            horiz.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);

            horiz.add(MsoyUI.createLabel((amount > 0) ? _msgs.buy() : _msgs.buyFree(), null));
            horiz.add(WidgetUtil.makeShim(_currency == Currency.BARS ? 10 : 5, 1));
            horiz.add(MsoyUI.createImage(_currency.getLargeIcon(), null));
            horiz.add(WidgetUtil.makeShim(_currency == Currency.BARS ? 10 : 5, 1));
            horiz.add(MsoyUI.createLabel(_currency.format(amount), null));

            setContent(horiz);
        }

        protected Currency _currency;
    }; // end: class BuyButton

    protected AsyncCallback<T> _callback;
    protected PriceQuote _quote;

    protected FlowPanel _barPanel, _coinPanel;
    protected BuyButton _buyBars, _buyCoins;
    protected HTML _barLabel;
    protected Widget _switchToCoins;

    /** Are we showing the "alternate currency" view? (showing a bar price for a coin item) */
    protected boolean _altCurrency;

    protected int _timesBought;

    protected static final MoneyMessages _msgs = GWT.create(MoneyMessages.class);
}
