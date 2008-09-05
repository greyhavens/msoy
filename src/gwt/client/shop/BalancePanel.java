//
// $Id$

package client.shop;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;

import org.gwtwidgets.client.util.SimpleDateFormat;

import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.money.data.all.MoneyHistory;
import com.threerings.msoy.money.data.all.Currency;

import client.ui.MsoyUI;

public class BalancePanel extends PagedGrid<MoneyHistory>
{
    public BalancePanel (int memberId, Currency currency)
    {
        super(10, 1, PagedGrid.NAV_ON_TOP);

        addStyleName("Balance");

        setModel(new MoneyHistoryDataModel(memberId, currency), 0);
    }

// TODO: This should work
//    @Override
//    public boolean padToFullPage ()
//    {
//        return true;
//    }

    @Override // from PagedGrid
    protected void displayResults (int start, int count, List<MoneyHistory> list)
    {
        super.displayResults(start, count, list);

        SmartTable footer = new SmartTable("Footer", 0, 0);
        footer.setWidth("100%");
        footer.setHTML(0, 0, "&nbsp;", 1, "BottomLeft");
        footer.setHTML(0, 1, "&nbsp;");
        footer.setHTML(0, 2, "&nbsp;", 1, "BottomRight");
        add(footer);
    }

    public Widget createWidget (MoneyHistory entry)
    {
        return new TransactionWidget(entry);
    }

    public String getEmptyMessage ()
    {
        return _msgs.transactionsNone();
    }

    protected static class TransactionWidget extends HorizontalPanel
    {
        public TransactionWidget (MoneyHistory entry)
        {
            addStyleName("Transaction");

            add(MsoyUI.createLabel(_format.format(entry.getTimestamp()), "Time"));
            add(MsoyUI.createLabel(_lookup.xlate(entry.getDescription()), "Description"));

            String debit, credit;
            if (entry.isSpent()) {
                debit = String.valueOf(entry.getAmount());
                credit = " ";
            } else {
                debit = " ";
                credit = String.valueOf(entry.getAmount());
            }
            add(MsoyUI.createLabel(debit, "Debit"));
            add(MsoyUI.createLabel(credit, "Credit"));
        }
    }

    // TODO: Unify with Mail date format?
    protected static SimpleDateFormat _format = new SimpleDateFormat("MM dd YY, h:mm a");

    protected static final ShopMessagesLookup _lookup = GWT.create(ShopMessagesLookup.class);
    protected static final ShopMessages _msgs = GWT.create(ShopMessages.class);
}
