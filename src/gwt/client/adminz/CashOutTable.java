//
// $Id$

package client.adminz;

import java.util.List;

import client.shell.Args;
import client.shell.Pages;
import client.ui.MsoyUI;
import client.ui.NumberTextBox;
import client.util.Link;
import client.util.ServiceUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.gwt.ui.Anchor;
import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.util.SimpleDataModel;
import com.threerings.msoy.money.data.all.CashOutEntry;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.gwt.MoneyService;
import com.threerings.msoy.money.gwt.MoneyServiceAsync;

public class CashOutTable extends PagedGrid<CashOutEntry>
{
    public CashOutTable ()
    {
        super(10, 1, NAV_ON_BOTTOM);
        init();
    }

    @Override // from PagedGrid
    protected Widget createWidget (CashOutEntry entry)
    {
        return new CashOutWidget(entry);
    }

    @Override // from PagedGrid
    protected String getEmptyMessage ()
    {
        return "<Empty>";
    }

    @Override // from PagedGrid
    protected boolean displayNavi (int items)
    {
        return true;
    }
    
    protected void init ()
    {
        addStyleName("dottedGrid");
        
        _moneysvc.getBlingCashOutRequests(new AsyncCallback<List<CashOutEntry>>() {
            public void onFailure (Throwable caught) {
                
            }
            public void onSuccess (List<CashOutEntry> result) {
                setModel(new SimpleDataModel<CashOutEntry>(result), 0);
            }
        });
    }

    protected class CashOutWidget extends SmartTable
    {
        public CashOutWidget (final CashOutEntry item)
        {
            super("cashOutWidget", 0, 3);
            
            setWidget(0, 0, Link.create(item.displayName, Pages.PEOPLE,
                         Integer.toString(item.memberId)), 1, "Name");
            
            setWidget(1, 0, new Anchor("mailto:" + item.emailAddress, item.emailAddress));
            setText(2, 0, "Amount: " + Currency.BLING.format(item.cashOutInfo.blingAmount));
            setText(3, 0, "Worth: " + formatUSD(item.cashOutInfo.blingWorth));
            setText(4, 0, "Requested: " + MsoyUI.formatDateTime(item.cashOutInfo.timeRequested));
            setText(0, 1, item.cashOutInfo.billingInfo.firstName + ' ' + 
                item.cashOutInfo.billingInfo.lastName);
            setText(1, 1, "PayPal: " + item.cashOutInfo.billingInfo.paypalEmailAddress);
            setText(2, 1, item.cashOutInfo.billingInfo.streetAddress);
            setText(3, 1, item.cashOutInfo.billingInfo.city + ", " +
                item.cashOutInfo.billingInfo.state + ' ' +
                item.cashOutInfo.billingInfo.country + ' ' +
                item.cashOutInfo.billingInfo.postalCode);
            setText(4, 1, item.cashOutInfo.billingInfo.phoneNumber);
            
            SmartTable extras = new SmartTable("Extras", 0, 5);
            Button btn = new Button("Transactions");
            btn.addStyleName("sideButton");
            btn.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    Link.go(Pages.ME, Args.compose("transactions", "3", Integer.toString(item.memberId)));
                }
            });
            extras.addWidget(btn, 0, null);
            btn = new Button("Cash Out");
            btn.addStyleName("sideButton");
            btn.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    showPanel(new CashOutPanel(Currency.BLING.format(item.cashOutInfo.blingAmount)));
                }
            });
            extras.addWidget(btn, 0, null);
            btn = new Button("Cancel Request");
            btn.addStyleName("sideButton");
            btn.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    showPanel(new CancelRequestPanel());
                }
            });
            extras.addWidget(btn, 0, null);
            setWidget(0, 2, extras);
            getFlexCellFormatter().setRowSpan(0, 2, getRowCount());
            getFlexCellFormatter().setHorizontalAlignment(0, 2, HasAlignment.ALIGN_RIGHT);
            getFlexCellFormatter().setVerticalAlignment(0, 2, HasAlignment.ALIGN_TOP);
        }
        
        protected void showPanel (Panel panel)
        {
            setWidget(5, 0, panel, 3, "PopupPanel");
        }
        
        protected class CancelRequestPanel extends SmartTable
        {
            public CancelRequestPanel ()
            {
                setText(0, 0, "Reason: ", 0, "PopupCell");
                
                setWidget(0, 1, _reasonBox = new TextArea(), 0, "PopupCell");
                _reasonBox.addStyleName("Reason");
                
                Button btn = new Button("Cancel this Request");
                btn.addStyleName("PopupButton");
                setWidget(0, 2, btn, 0, "PopupCell");
            }
            
            @Override
            protected void onLoad ()
            {
                super.onLoad();
                _reasonBox.setFocus(true);
            }
            
            protected TextArea _reasonBox;
        }
        
        protected class CashOutPanel extends FlowPanel
        {
            public CashOutPanel (String initialAmount)
            {
                add(new InlineLabel("Amount to cash out: "));
                
                _amountBox = new NumberTextBox(true);
                _amountBox.setText(initialAmount);
                add(_amountBox);
                
                Button btn = new Button("Cash Out This Member");
                btn.addStyleName("PopupButton");
                add(btn);
            }
            
            @Override
            protected void onLoad ()
            {
                super.onLoad();
                _amountBox.setSelectionRange(0, _amountBox.getText().length());
                _amountBox.setFocus(true);
            }
            
            protected NumberTextBox _amountBox;
        }
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
    
    protected static final AdminMessages _msgs = GWT.create(AdminMessages.class);
    
    protected static final MoneyServiceAsync _moneysvc = (MoneyServiceAsync)
        ServiceUtil.bind(GWT.create(MoneyService.class), MoneyService.ENTRY_POINT);
}
