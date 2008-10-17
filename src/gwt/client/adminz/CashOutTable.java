//
// $Id$

package client.adminz;

import java.util.List;

import client.shell.CShell;
import client.ui.MsoyUI;
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
import com.google.gwt.user.client.ui.TextBox;
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
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

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
        return _msgs.cashOutEmptyMessage();
    }

    @Override // from PagedGrid
    protected boolean displayNavi (int items)
    {
        return true;
    }
    
    protected void init ()
    {
        addStyleName("dottedGrid");
        reload();
    }
    
    protected void reload ()
    {
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
            
            this.entry = item;
            
            setWidget(0, 0, Link.create(item.displayName, Pages.PEOPLE,
                         Integer.toString(item.memberId)), 1, "Name");
            
            setWidget(1, 0, new Anchor("mailto:" + item.emailAddress, item.emailAddress));
            setText(2, 0, _msgs.cashOutEntryAmount(Currency.BLING.format(item.cashOutInfo.blingAmount)));
            setText(3, 0, _msgs.cashOutEntryWorth(formatUSD(item.cashOutInfo.blingWorth)));
            setText(4, 0, _msgs.cashOutEntryRequested(MsoyUI.formatDateTime(item.cashOutInfo.timeRequested)));
            setText(0, 1, item.cashOutInfo.billingInfo.firstName + ' ' + 
                item.cashOutInfo.billingInfo.lastName);
            setText(1, 1, _msgs.cashOutEntryPayPal(item.cashOutInfo.billingInfo.paypalEmailAddress));
            setText(2, 1, item.cashOutInfo.billingInfo.streetAddress);
            setText(3, 1, item.cashOutInfo.billingInfo.city + ", " +
                item.cashOutInfo.billingInfo.state + ' ' +
                item.cashOutInfo.billingInfo.country + ' ' +
                item.cashOutInfo.billingInfo.postalCode);
            setText(4, 1, item.cashOutInfo.billingInfo.phoneNumber);
            
            SmartTable extras = new SmartTable("Extras", 0, 5);
            Button btn = new Button(_msgs.cashOutEntryTransactionsButton());
            btn.addStyleName("sideButton");
            btn.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    Link.go(Pages.ME, Args.compose("transactions", "3", Integer.toString(item.memberId)));
                }
            });
            extras.addWidget(btn, 0, null);
            btn = new Button(_msgs.cashOutEntryCashOutButton());
            btn.addStyleName("sideButton");
            btn.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    showPanel(new CashOutPanel(item.cashOutInfo.blingAmount));
                }
            });
            extras.addWidget(btn, 0, null);
            btn = new Button(_msgs.cashOutEntryCancelButton());
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
                
                Button btn = new Button(_msgs.cashOutEntryCancelButton());
                btn.addStyleName("PopupButton");
                btn.addClickListener(new ClickListener() {
                    public void onClick (Widget sender) {
                        doCancel();
                    }
                });
                setWidget(0, 2, btn, 0, "PopupCell");
            }
            
            @Override
            protected void onLoad ()
            {
                super.onLoad();
                _reasonBox.setFocus(true);
            }
            
            protected void doCancel ()
            {
                _moneysvc.cancelCashOut(entry.memberId, _reasonBox.getText(), new AsyncCallback<Void>() {
                    public void onFailure (Throwable cause) {
                        MsoyUI.error(CShell.serverError(cause));
                    }
                    public void onSuccess (Void result) {
                        reload();
                        MsoyUI.info(_msgs.cashOutEntryCancelSuccess());
                    }
                });
            }
            
            protected TextArea _reasonBox;
        }
        
        protected class CashOutPanel extends FlowPanel
        {
            public CashOutPanel (int initialAmount)
            {
                add(new InlineLabel(_msgs.cashOutEntryCashOutPanelAmount()));
                
                _amountBox = new TextBox();
                _amountBox.setText(Currency.BLING.format(initialAmount));
                add(_amountBox);
                
                Button btn = new Button(_msgs.cashOutEntryCashOutPanelButton());
                btn.addStyleName("PopupButton");
                btn.addClickListener(new ClickListener() {
                    public void onClick (Widget sender) {
                        doCashOut();
                    }
                });
                add(btn);
                
                add(_status = new InlineLabel(""));
            }
            
            @Override
            protected void onLoad ()
            {
                super.onLoad();
                _amountBox.setSelectionRange(0, _amountBox.getText().length());
                _amountBox.setFocus(true);
            }
            
            protected void doCashOut()
            {
                int blingAmount = Currency.BLING.parse(_amountBox.getText());
                _moneysvc.cashOutBling(entry.memberId, blingAmount, new AsyncCallback<Void>() {
                    public void onFailure (Throwable cause) {
                        MsoyUI.error(CShell.serverError(cause));
                    }
                    public void onSuccess (Void result) {
                        reload();
                        MsoyUI.info(_msgs.cashOutEntryCashOutSuccess());
                    }
                });
            }
            
            protected final TextBox _amountBox;
            protected final InlineLabel _status;
        }
        
        protected final CashOutEntry entry;
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
