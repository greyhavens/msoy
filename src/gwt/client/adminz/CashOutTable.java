//
// $Id$

package client.adminz;

import java.util.List;

import client.shell.Pages;
import client.ui.MsoyUI;
import client.util.Link;
import client.util.ServiceUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.gwt.ui.Anchor;
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

    protected static class CashOutWidget extends SmartTable
    {
        public CashOutWidget (CashOutEntry item)
        {
            super("cashOutWidget", 0, 5);
//            setWidget(0, 1, Link.create(item.displayName, Pages.PEOPLE,
//                Integer.toString(item.memberId)), 1, "Name");
            
            setWidget(0, 0, Link.create(item.displayName, Pages.PEOPLE,
                         Integer.toString(item.memberId)), 1, "Name");
            
            // we'll overwrite these below if we have anything to display
            getFlexCellFormatter().setStyleName(1, 0, "Status");
            setText(1, 0, "Cash out info");
            setText(1, 1, "Payment info");
            
            SmartTable extras = new SmartTable("Extras", 0, 5);
            int row = 0;
            
            // if we're not a guest, we can send them mail
            extras.setWidget(row, 0, new Anchor("mailto:" + item.emailAddress, item.emailAddress), 
                2, null);
            
            // Commit button
            extras.setWidget(row++, 2, new Button("Cash Out"));
            
            // Transactions link
            extras.setWidget(row, 0,
                MsoyUI.createActionImage(Currency.BLING.getSmallIcon(), null));
            extras.setWidget(row, 1,
                MsoyUI.createActionLabel("Transactions", null));
            
            extras.setWidget(row++, 2, new Button("Cancel"));
            
            setWidget(0, 2, extras);
            getFlexCellFormatter().setRowSpan(0, 2, getRowCount());
            getFlexCellFormatter().setHorizontalAlignment(0, 2, HasAlignment.ALIGN_RIGHT);
            getFlexCellFormatter().setVerticalAlignment(0, 2, HasAlignment.ALIGN_TOP);
        }
    }
    
    protected static final AdminMessages _msgs = GWT.create(AdminMessages.class);
    
    protected static final MoneyServiceAsync _moneysvc = (MoneyServiceAsync)
        ServiceUtil.bind(GWT.create(MoneyService.class), MoneyService.ENTRY_POINT);
}
