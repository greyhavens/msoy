//
// $Id$

package client.me;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.msoy.money.data.all.BlingExchangeResult;
import com.threerings.msoy.money.data.all.BlingInfo;
import com.threerings.msoy.money.data.all.CashOutBillingInfo;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.gwt.MoneyService;
import com.threerings.msoy.money.gwt.MoneyServiceAsync;

import client.shell.CShell;
import client.shell.ShellMessages;
import client.ui.NumberTextBox;
import client.util.ServiceUtil;
import client.util.StringUtil;
import client.util.events.StatusChangeEvent;

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
        setWidget(row++, 0, _cashedOutBling = new Label(), 3, "Success");
        setText(row++, 1, " ", 3, null);
        setText(row++, 0, _msgs.exchangeBlingForBars(), 3, "header");
        setText(row++, 0, _msgs.exchangeBlingDescription(), 3, null);
        setText(row, 0, _msgs.exchangeAmount(), 1, "rightLabel");
        setWidget(row, 1, _exchangeBox = new NumberTextBox(true));
        setWidget(row++, 2, _exchangeBtn = new Button(_msgs.exchangeButton(), new ClickListener() {
            public void onClick (Widget sender) {
                doExchange(memberId);
            }
        }));
        setWidget(row++, 1, _exchangeStatus = new Label(""), 2, null);
        setText(row++, 0, _msgs.blingCashOutHeader(), 3, "header");
        setText(row++, 0, _msgs.blingCashOutDescription(), 3, null);
        setText(row, 0, _msgs.fieldTemplate(_msgs.cashOutPassword()), 1, "rightLabel");
        setWidget(row++, 1, _passwordBox = new PasswordTextBox());
        setText(row, 0, _msgs.fieldTemplate(_msgs.cashOutPayPalEmail()), 1, "rightLabel");
        setWidget(row++, 1, _paypalEmailBox = new TextBox());
        _paypalEmailBox.setText(CMe.creds.accountName);
        setText(row, 0, _msgs.fieldTemplate(_msgs.cashOutConfirmPayPalEmail()), 1, "rightLabel");
        setWidget(row++, 1, _paypalEmailConfirmBox = new TextBox());
        setText(row, 0, _msgs.fieldTemplate(_msgs.cashOutFirstName()), 1, "rightLabel");
        setWidget(row++, 1, _firstNameBox = new TextBox());
        setText(row, 0, _msgs.fieldTemplate(_msgs.cashOutLastName()), 1, "rightLabel");
        setWidget(row++, 1, _lastNameBox = new TextBox());
        setText(row, 0, _msgs.fieldTemplate(_msgs.cashOutPhoneNumber()), 1, "rightLabel");
        setWidget(row++, 1, _phoneNumberBox = new TextBox());
        setText(row, 0, _msgs.fieldTemplate(_msgs.cashOutStreetAddress()), 1, "rightLabel");
        setWidget(row++, 1, _streetAddressBox = new TextBox());
        setText(row, 0, _msgs.fieldTemplate(_msgs.cashOutCity()), 1, "rightLabel");
        setWidget(row++, 1, _cityBox = new TextBox());
        setText(row, 0, _msgs.fieldTemplate(_msgs.cashOutState()), 1, "rightLabel");
        setWidget(row++, 1, _stateBox = new TextBox());
        setText(row, 0, _msgs.fieldTemplate(_msgs.cashOutPostalCode()), 1, "rightLabel");
        setWidget(row++, 1, _postalCodeBox = new TextBox());
        setText(row, 0, _msgs.fieldTemplate(_msgs.cashOutCountry()), 1, "rightLabel");
        setWidget(row++, 1, _countryBox = new TextBox());
        
        setText(row, 0, _msgs.blingCashOutAmount(), 1, "rightLabel");
        setWidget(row, 1, _cashOutBox = new NumberTextBox(true));
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
        if (result.cashOut != null) {
            _cashedOutBling.setText(_msgs.cashedOutBling(
                Currency.BLING.format(result.cashOut.blingAmount),
                formatUSD(result.cashOut.blingWorth)));
        } else {
            _cashedOutBling.setText("");
        }
    }
    
    protected void doExchange (int memberId)
    {
        // Validate the data
        int blingAmount = getValidAmount(_exchangeBox, _exchangeStatus, _msgs.blingInvalidAmount());
        if (blingAmount == 0) {
            return;
        }
        
        // Ensure the amount is valid.
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
        // Validate the data
        int blingAmount = getValidAmount(_cashOutBox, _cashOutStatus, _msgs.blingInvalidAmount());
        if (blingAmount == 0) {
            return;
        }
        if (!requireField(_passwordBox.getText(), _cashOutStatus, _msgs.cashOutPassword()) ||
            !requireField(_firstNameBox.getText(), _cashOutStatus, _msgs.cashOutFirstName()) ||
            !requireField(_lastNameBox.getText(), _cashOutStatus, _msgs.cashOutLastName()) ||
            !requireField(_paypalEmailBox.getText(), _cashOutStatus, _msgs.cashOutPayPalEmail()) ||
            !requireField(_phoneNumberBox.getText(), _cashOutStatus, _msgs.cashOutPhoneNumber()) ||
            !requireField(_streetAddressBox.getText(), _cashOutStatus, _msgs.cashOutStreetAddress()) ||
            !requireField(_cityBox.getText(), _cashOutStatus, _msgs.cashOutCity()) ||
            !requireField(_stateBox.getText(), _cashOutStatus, _msgs.cashOutState()) ||
            !requireField(_postalCodeBox.getText(), _cashOutStatus, _msgs.cashOutPostalCode()) ||
            !requireField(_countryBox.getText(), _cashOutStatus, _msgs.cashOutCountry())) {
            return;
        }
        if (!_paypalEmailBox.getText().equals(_paypalEmailConfirmBox.getText())) {
            setError(_cashOutStatus, _msgs.cashOutEmailsDontMatch());
            return;
        }
        
        // Ensure the amount is valid.
        _cashOutBtn.setEnabled(false);
        try {
            String password = CShell.frame.md5hex(_passwordBox.getText());
            _moneysvc.requestCashOutBling(memberId, blingAmount, password, 
                new CashOutBillingInfo(_firstNameBox.getText(), _lastNameBox.getText(), 
                _paypalEmailBox.getText(), _phoneNumberBox.getText(), _streetAddressBox.getText(), 
                _cityBox.getText(), _stateBox.getText(), _postalCodeBox.getText(), 
                _countryBox.getText()), new AsyncCallback<BlingInfo>() {
                public void onFailure (Throwable cause) {
                    setError(_cashOutStatus, CMe.serverError(cause));
                }
                public void onSuccess (BlingInfo result) {
                    setSuccess(_cashOutStatus, _msgs.cashOutRequestSuccessful());
                    _cashOutBox.setText("");
                    update(result);
                }
            });
        } finally {
            _cashOutBtn.setEnabled(true);
        }
    }
    
    protected void setSuccess (Label label, String message)
    {
        label.setText(message);
        label.removeStyleName("Error");
        label.addStyleName("Success");
    }
    
    protected void setError (Label label, String message)
    {
        label.setText(message);
        label.removeStyleName("Success");
        label.addStyleName("Error");
    }
    
    protected boolean requireField (String value, Label status, String fieldName)
    {
        if (StringUtil.isBlank(value)) {
            setError(status, _msgs.fieldRequired(fieldName));
            return false;
        }
        return true;
    }
    
    protected int getValidAmount (NumberTextBox box, Label status, String invalidMessage)
    {
        final int blingAmount = box.getValue().intValue();
        if (blingAmount < 1) {
            setError(status, invalidMessage);
            return 0;
        }
        return blingAmount;
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
    protected Label _cashedOutBling;
    
    protected NumberTextBox _exchangeBox;
    protected Button _exchangeBtn;
    protected Label _exchangeStatus;
    protected NumberTextBox _cashOutBox;
    protected PasswordTextBox _passwordBox;
    protected TextBox _firstNameBox;
    protected TextBox _lastNameBox;
    protected TextBox _paypalEmailBox;
    protected TextBox _paypalEmailConfirmBox;
    protected TextBox _phoneNumberBox;
    protected TextBox _streetAddressBox;
    protected TextBox _cityBox;
    protected TextBox _stateBox;
    protected TextBox _postalCodeBox;
    protected TextBox _countryBox;
    
    protected Button _cashOutBtn;
    protected Label _cashOutStatus;
    
    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final MeMessages _msgs = GWT.create(MeMessages.class);
    protected static final MoneyServiceAsync _moneysvc = (MoneyServiceAsync)
        ServiceUtil.bind(GWT.create(MoneyService.class), MoneyService.ENTRY_POINT);
}
