//
// $Id$

package client.me;

import client.shell.ShellMessages;
import client.util.ServiceUtil;
import client.util.events.StatusChangeEvent;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.msoy.money.data.all.BlingExchangeResult;
import com.threerings.msoy.money.data.all.BlingInfo;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.gwt.MoneyService;
import com.threerings.msoy.money.gwt.MoneyServiceAsync;

public class BlingPanel extends SmartTable
{
    public BlingPanel (final MoneyTransactionDataModel model)
    {
        setCellSpacing(10);
        setStyleName("bling");
        
        model.addBlingCallback(new AsyncCallback<BlingInfo>() {
            public void onFailure (Throwable caught) {
                // Ignore
            }
            public void onSuccess (BlingInfo result) {
                update(result);
            }
        });
        init(model.memberId);
    }
    
    protected void init (final int memberId)
    {
        int row = 0;
        setText(row++, 0, _msgs.blingHeader(), 3, "header");
        setText(row, 0, _msgs.blingBalance(), 1, "rightLabel");
        setWidget(row++, 1, _blingBalance = new Label());
        setText(row, 0, _msgs.blingWorth(), 1, "rightLabel");
        setWidget(row++, 1, _blingWorth = new Label());
        setText(row++, 1, " ", 3, null);
        setText(row++, 0, _msgs.exchangeBlingForBars(), 3, "header");
        setText(row++, 0, _msgs.exchangeBlingDescription(), 3, null);
        setText(row, 0, _msgs.exchangeAmount(), 1, "rightLabel");
        setWidget(row, 1, _exchangeBox = new TextBox());
        setWidget(row++, 2, _exchangeBtn = new Button(_msgs.exchangeButton(), new ClickListener() {
            public void onClick (Widget sender) {
                doExchange(memberId);
            }
        }));
        setWidget(row++, 1, _exchangeStatus = new Label(""), 2, null);
        setText(row++, 0, _msgs.blingCashOutHeader(), 3, "header");
        setText(row++, 0, _msgs.blingCashOutDescription(), 3, null);
        setText(row, 0, _msgs.blingCashOutAmount(), 1, "rightLabel");
        setWidget(row, 1, _cashOutBox = new TextBox());
        setWidget(row++, 2, _cashOutBtn = new Button(_msgs.blingCashOutButton(), new ClickListener() {
            public void onClick (Widget sender) {
                doCashOut(memberId);
            }
        }));
        setWidget(row++, 1, _cashOutStatus = new Label(""), 2, null);
    }
    
    protected void update (BlingInfo result)
    {
        _blingBalance.setText(Currency.BLING.format(result.bling));
        _blingWorth.setText(formatUSD(result.blingWorth));
    }
    
    protected void doExchange (int memberId)
    {
        // Ensure the amount is valid.
        final int blingAmount;
        try {
            blingAmount = Integer.parseInt(_exchangeBox.getText());
        } catch (Exception e) {
            setError(_exchangeStatus, _msgs.blingInvalidAmount());
            return;
        }
        if (blingAmount < 1) {
            setError(_exchangeStatus, _msgs.blingInvalidAmount());
            return;
        }
        
        _exchangeBtn.setEnabled(false);
        try {
            _moneysvc.exchangeBlingForBars(memberId, blingAmount, 
                    new AsyncCallback<BlingExchangeResult>() {
                public void onFailure (Throwable cause) {
                    setError(_exchangeStatus, CMe.serverError(cause));
                }
                public void onSuccess (BlingExchangeResult result) {
                    setSuccess(_exchangeStatus, _msgs.blingExchangeSuccessful());
                    _exchangeBox.setText("");
                    update(result.blingInfo);
                    CMe.frame.dispatchEvent(new StatusChangeEvent(StatusChangeEvent.GOLD, 
                        result.barBalance, 0));
                }
            });
        } finally {
            _exchangeBtn.setEnabled(true);
        }
    }
    
    protected void doCashOut (int memberId)
    {
        
    }
    
    protected void setSuccess (Label label, String message)
    {
        label.setText(message);
        label.addStyleName("Success");
    }
    
    protected void setError (Label label, String message)
    {
        label.setText(message);
        label.addStyleName("Error");
    }
    
    /**
     * Converts the amount of pennies into a string to display to the user as a valid currency.
     * Note: there are some other utilities around to do this, but they're either in a different
     * project (and there's some concern about exposing them directly), or they don't properly
     * take into account floating-point round off errors.  This may get replaced or expanded
     * later on.
     */
    protected static String formatUSD (int pennies)
    {
        int dollars = pennies / 100;
        int cents = pennies % 100;
        return "USD $" + NumberFormat.getDecimalFormat().format(dollars) + '.' +
            (cents < 10 ? '0' : "") + cents;
    }

    protected Label _blingBalance;
    protected Label _blingWorth;
    
    protected TextBox _exchangeBox;
    protected Button _exchangeBtn;
    protected Label _exchangeStatus;
    protected TextBox _cashOutBox;
    protected Button _cashOutBtn;
    protected Label _cashOutStatus;
    
    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final MeMessages _msgs = GWT.create(MeMessages.class);
    protected static final MoneyServiceAsync _moneysvc = (MoneyServiceAsync)
        ServiceUtil.bind(GWT.create(MoneyService.class), MoneyService.ENTRY_POINT);
}
