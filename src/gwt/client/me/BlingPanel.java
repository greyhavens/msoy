//
// $Id$

package client.me;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.msoy.money.data.all.BlingInfo;
import com.threerings.msoy.money.data.all.CashOutBillingInfo;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.MoneyResult;
import com.threerings.msoy.money.gwt.MoneyService;
import com.threerings.msoy.money.gwt.MoneyServiceAsync;

import client.shell.CShell;
import client.shell.ShellMessages;
import client.ui.MsoyUI;
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
        
        _model = model;
        init();
    }
    
    protected void init ()
    {
        clear();

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
        setWidget(row, 1, _exchangeBox = new NumberTextBox(true));
        setWidget(row++, 2, _exchangeBtn = new Button(_msgs.exchangeButton(), new ClickListener() {
            public void onClick (Widget sender) {
                doExchange(_model.memberId);
            }
        }));
        setText(row++, 0, _msgs.blingCashOutHeader(), 3, "header");
        _cashOutRow = row;

        _model.addBlingCallback(new AsyncCallback<BlingInfo>() {
            public void onFailure (Throwable caught) {
                // Ignore
            }
            public void onSuccess (BlingInfo result) {
                update(result);
            }
        });
    }
    
    protected void update (BlingInfo result)
    {
        _blingBalance.setText(Currency.BLING.format(result.bling));
        _blingWorth.setText(formatUSD((int)(result.worthPerBling * result.bling)));
        if (result.cashOut != null) {
            setWidget(_cashOutRow, 0, new Label(_msgs.cashedOutBling(
                Currency.BLING.format(result.cashOut.blingAmount),
                formatUSD(result.cashOut.blingWorth))), 3, "Success");
            setWidget(_cashOutRow, 1, new Button(_cmsgs.cancel(), new ClickListener() {
                public void onClick (final Widget sender) {
                    _moneysvc.cancelCashOut(_model.memberId, "m.user_cancelled", new AsyncCallback<Void>() {
                        public void onFailure (Throwable cause) {
                            MsoyUI.errorNear(CShell.serverError(cause), sender);
                        }
                        public void onSuccess (Void v) {
                            MsoyUI.info(_msgs.cancelCashOutSuccess());
                            init();
                        }
                    });
                }
            }));
        } else {
            if (result.bling >= result.minCashOutBling) {
                setWidget(_cashOutRow, 0, new CashOutForm(result.worthPerBling), 3, "bling");
            } else {
                setWidget(_cashOutRow, 0, new Label(_msgs.cashOutBelowMinimum(
                    Float.toString(result.minCashOutBling/100.0f))), 3, "Error");
            }
            _cashedOutBling.setText("");
        }
    }
    
    protected class CashOutForm extends SmartTable
    {
        public CashOutForm (final float worthPerBling)
        {
            setCellSpacing(10);
            
            int row = 0;
            setText(row++, 0, _msgs.blingCashOutDescription(), 3, null);
            setText(row, 0, _msgs.blingCashOutAmount(), 1, "rightLabel");
            FlowPanel panel = new FlowPanel();
            final Label worthLabel = new InlineLabel();
            panel.add(_cashOutBox = new NumberTextBox(true));
            panel.add(worthLabel);
            setWidget(row++, 1, panel, 2, null);
            _cashOutBox.addKeyboardListener(new KeyboardListenerAdapter() {
                public void onKeyUp (Widget sender, char keyCode, int modifiers) {
                    worthLabel.setText(_msgs.cashOutAmountWorth(formatUSD(
                        (int)(_cashOutBox.getValue().floatValue() * 100.0f * worthPerBling))));
                }
            });
            setText(row, 0, _msgs.fieldTemplate(_msgs.cashOutPassword()), 1, "rightLabel");
            setWidget(row++, 1, _passwordBox = new PasswordTextBox());
            setText(row, 0, _msgs.fieldTemplate(_msgs.cashOutPayPalEmail()), 1, "rightLabel");
            setWidget(row++, 1, _paypalEmailBox = new TextBox());
            _paypalEmailBox.setText(CShell.creds.accountName);
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
            setWidget(row++, 2, _cashOutBtn =
                      new Button(_msgs.blingCashOutButton(), new ClickListener() {
                public void onClick (Widget sender) {
                    doCashOut(_model.memberId);
                }
            }));
        }
        
        protected void doCashOut (int memberId)
        {
            // Validate the data
            int blingAmount = getValidAmount(_cashOutBox, _msgs.blingInvalidAmount());
            if (blingAmount == 0) {
                return;
            }
            if (!requireField(_passwordBox, _msgs.cashOutPassword()) ||
                !requireField(_firstNameBox, _msgs.cashOutFirstName()) ||
                !requireField(_lastNameBox, _msgs.cashOutLastName()) ||
                !requireField(_paypalEmailBox, _msgs.cashOutPayPalEmail()) ||
                !requireField(_phoneNumberBox, _msgs.cashOutPhoneNumber()) ||
                !requireField(_streetAddressBox, _msgs.cashOutStreetAddress()) ||
                !requireField(_cityBox, _msgs.cashOutCity()) ||
                !requireField(_stateBox, _msgs.cashOutState()) ||
                !requireField(_postalCodeBox, _msgs.cashOutPostalCode()) ||
                !requireField(_countryBox, _msgs.cashOutCountry())) {
                return;
            }
            if (!_paypalEmailBox.getText().equals(_paypalEmailConfirmBox.getText())) {
                MsoyUI.errorNear(_msgs.cashOutEmailsDontMatch(), _paypalEmailConfirmBox);
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
                        MsoyUI.error(CShell.serverError(cause));
                    }
                    public void onSuccess (BlingInfo result) {
                        MsoyUI.info(_msgs.cashOutRequestSuccessful());
                        _cashOutBox.setText("");
                        update(result);
                    }
                });
            } finally {
                _cashOutBtn.setEnabled(true);
            }
        }
        
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
    }
    
    protected void doExchange (int memberId)
    {
        // Validate the data
        int blingAmount = getValidAmount(_exchangeBox, _msgs.blingInvalidAmount());
        if (blingAmount == 0) {
            return;
        }
        
        // Ensure the amount is valid.
        _exchangeBtn.setEnabled(false);
        _moneysvc.exchangeBlingForBars(memberId, blingAmount, new AsyncCallback<MoneyResult>() {
            public void onFailure (Throwable cause) {
                _exchangeBtn.setEnabled(true);
                MsoyUI.errorNear(CShell.serverError(cause), _exchangeBox);
            }
            public void onSuccess (MoneyResult result) {
                _exchangeBtn.setEnabled(true);
                MsoyUI.info(_msgs.blingExchangeSuccessful());
                _exchangeBox.setText("");
                update(result.blingInfo);
                CShell.frame.dispatchEvent(new StatusChangeEvent(StatusChangeEvent.BARS, 
                    result.barBalance, 0));
            }
        });
    }
    
    protected boolean requireField (TextBox box, String fieldName)
    {
        if (StringUtil.isBlank(box.getText())) {
            MsoyUI.errorNear(_msgs.fieldRequired(fieldName), box);
            return false;
        }
        return true;
    }
    
    protected int getValidAmount (NumberTextBox box, String invalidMessage)
    {
        final int blingAmount = box.getValue().intValue();
        if (blingAmount < 1) {
            MsoyUI.errorNear(invalidMessage, box);
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
    
    protected int _cashOutRow;

    protected MoneyTransactionDataModel _model;
    
    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final MeMessages _msgs = GWT.create(MeMessages.class);
    protected static final MoneyServiceAsync _moneysvc = (MoneyServiceAsync)
        ServiceUtil.bind(GWT.create(MoneyService.class), MoneyService.ENTRY_POINT);
}
