//
// $Id: BuyPanel.java 14685 2009-02-05 02:41:10Z ray $

package client.money;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.SourcesClickEvents;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.Anchor;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.money.gwt.CostUpdatedException;
import com.threerings.msoy.money.gwt.InsufficientFundsException;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.PriceQuote;
import com.threerings.msoy.money.data.all.PurchaseResult;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.CShell;
import client.ui.MsoyUI;
import client.ui.RoundBox;
import client.ui.StretchButton;
import client.util.BillingURLs;
import client.util.ClickCallback;
import client.util.MoneyUtil;
import client.util.Link;

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
        Label prompt = new Label(promptStr);
        prompt.setStyleName("BuyPrompt");
        box.add(prompt);
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
        setStyleName("Buy");

        // Buy with bars, plus a link on how to acquire some
        _buyBars = new BuyButton(Currency.BARS);
        _barPanel = new FlowPanel();
        _barPanel.add(_buyBars);
        _barLabel = new Label();
        _barPanel.add(_barLabel);
        getFlexCellFormatter().setColSpan(0, 0, 2);
        setWidget(0, 0, _barPanel);

        _getBars = MsoyUI.createButton(MsoyUI.MEDIUM_THIN, _msgs.getBars(), new ClickListener() {
            public void onClick (Widget sender) {
                Window.open(BillingURLs.getEntryPoint(CShell.creds), "_blank", "");
            }
        });
        _getBars.addStyleName("buyPanelButton");
        setWidget(1, 0, _getBars);

        _buyCoins = new BuyButton(Currency.COINS);
        setWidget(1, 1, _buyCoins);

        _addenda = new FlowPanel();
        setWidget(2, 0, _addenda);
        getFlexCellFormatter().setColSpan(2, 0, 2);
        
        // Display exchange rate 
        _wikiLink = MsoyUI.createExternalAnchor("http://wiki.whirled.com/Currency", "");
        
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
        setStyleName("Bought");

        FlowPanel boughtPanel = new FlowPanel();
        addPurchasedUI(result.ware, boughtPanel);

        if (result.charity != null) {
            String percentage = NumberFormat.getPercentFormat().format(
                (int)(100.0 * result.charityPercentage) / 100.0);
            boughtPanel.add(WidgetUtil.makeShim(10, 10));
            FlowPanel charityPanel = new FlowPanel();
            charityPanel.setStyleName("Charity");
            charityPanel.add(new InlineLabel(_msgs.donatedToCharity(percentage)));
            charityPanel.add(Link.create(
                result.charity.toString(), Pages.PEOPLE, ""+result.charity.getMemberId()));
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

        _barPanel.setVisible(quote.getCoins() > 0);
        _buyBars.setAmount(quote.getBars());
        if (quote.getListedCurrency() == Currency.BARS) {
            _barLabel.setText(_msgs.barCost(NumberFormat.getCurrencyFormat().format(
                quote.getUSDPerBarCost() * quote.getBars())));
            _addenda.remove(_wikiLink);
        } else {
            _barLabel.setText(quote.getCoinChange() > 0 ?
                _msgs.coinChange(Currency.COINS.format(quote.getCoinChange())) : "");
            _wikiLink.setText(_msgs.exchangeRate(Currency.COINS.format(
                (int)Math.floor(quote.getExchangeRate()))));
            _addenda.add(_wikiLink);
        }
        _buyCoins.setAmount(quote.getCoins());
    }

    protected class BuyCallback extends ClickCallback<PurchaseResult<T>>
    {
        public BuyCallback (SourcesClickEvents button, Currency currency)
        {
            super(button);
            _currency = currency;
        }

        @Override protected boolean callService ()
        {
            return makePurchase(_currency, _quote.getAmount(_currency), this);
        }

        @Override protected boolean gotResult (PurchaseResult<T> result)
        {
            _timesBought++;
            MoneyUtil.updateBalances(result.balances);
            if (result.quote != null) {
                updatePrice(result.quote);
            }
            wasPurchased(result, _currency);
            if (_callback != null) {
                _callback.onSuccess(result.ware);
            }
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
            super(currency == Currency.BARS ? ORANGE_THICK : BLUE_THIN, null);
            _currency = currency;
            addStyleName("buyPanelButton");
            new BuyCallback(this, currency);
        }

        public void setAmount (int amount)
        {
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

    protected BuyButton _buyBars, _buyCoins;
    protected PushButton _getBars;
    protected FlowPanel _barPanel;
    protected FlowPanel _addenda;
    protected Label _barLabel;
    protected Anchor _wikiLink;
    
    protected int _timesBought;

    protected static final MoneyMessages _msgs = GWT.create(MoneyMessages.class);
}
