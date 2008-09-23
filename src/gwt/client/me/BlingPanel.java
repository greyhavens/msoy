//
// $Id$

package client.me;

import client.shell.ShellMessages;
import client.util.ServiceUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.gwt.ui.SmartTable;
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
                init(result, model.memberId);
            }
        });
    }
    
    protected void init (BlingInfo result, final int memberId)
    {
        int row = 0;
        setText(row++, 0, _msgs.blingHeader(), 3, "header");
        setText(row, 0, _msgs.blingBalance(), 1, "rightLabel");
        setText(row++, 1, Currency.BLING.format(result.bling));
        setText(row, 0, _msgs.blingWorth(), 1, "rightLabel");
        setText(row++, 1, formatUSD(result.blingWorth));
        setText(row++, 1, " ", 3, null);
        setText(row++, 0, _msgs.exchangeBlingForBars(), 3, "header");
        setText(row++, 0, _msgs.exchangeBlingDescription(), 3, null);
        setText(row, 0, _msgs.exchangeAmount(), 1, "rightLabel");
        setWidget(row, 1, _amountBox = new TextBox());
        setWidget(row++, 2, _exchangeBtn = new Button(_msgs.exchangeButton(), new ClickListener() {
            public void onClick (Widget sender) {
                doExchange(memberId);
            }
        }));
        setWidget(row, 1, _exchangeStatus = new Label(""));
    }
    
    protected void doExchange (int memberId)
    {
        // Ensure the amount is valid.
        final int blingAmount;
        try {
            blingAmount = Integer.parseInt(_amountBox.getText());
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
            _moneysvc.exchangeBlingForBars(memberId, blingAmount, new AsyncCallback<Void>() {
                public void onFailure (Throwable cause) {
                    setError(_exchangeStatus, CMe.serverError(cause));
                }
                public void onSuccess (Void result) {
                    setSuccess(_exchangeStatus, _msgs.blingExchangeSuccessful());
                    _amountBox.setText("");
                }
            });
        } finally {
            _exchangeBtn.setEnabled(true);
        }
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

    protected TextBox _amountBox;
    protected Button _exchangeBtn;
    protected Label _exchangeStatus;
    
    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final MeMessages _msgs = GWT.create(MeMessages.class);
    protected static final MoneyServiceAsync _moneysvc = (MoneyServiceAsync)
        ServiceUtil.bind(GWT.create(MoneyService.class), MoneyService.ENTRY_POINT);
}
