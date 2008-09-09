//
// $Id$

package client.me;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.money.data.all.MoneyTransaction;
import com.threerings.msoy.money.data.all.ReportType;

import client.ui.MsoyUI;

public class BalancePanel extends PagedGrid<MoneyTransaction>
{
    public BalancePanel (int memberId, ReportType report)
    {
        super(10, 1, PagedGrid.NAV_ON_TOP);

        addStyleName("Balance");

        setModel(new MoneyTransactionDataModel(memberId, report), 0);
    }

// TODO: This should work
//    @Override
//    public boolean padToFullPage ()
//    {
//        return true;
//    }

    @Override // from PagedGrid
    protected void displayResults (int start, int count, List<MoneyTransaction> list)
    {
        super.displayResults(start, count, list);

        SmartTable footer = new SmartTable("Footer", 0, 0);
        footer.setWidth("100%");
        footer.setHTML(0, 0, "&nbsp;", 1, "BottomLeft");
        footer.setHTML(0, 1, "&nbsp;");
        footer.setHTML(0, 2, "&nbsp;", 1, "BottomRight");
        add(footer);
    }

    public Widget createWidget (MoneyTransaction entry)
    {
        return new TransactionWidget(entry);
    }

    public String getEmptyMessage ()
    {
        return _msgs.transactionsNone();
    }

    protected static class TransactionWidget extends HorizontalPanel
    {
        public TransactionWidget (MoneyTransaction entry)
        {
            addStyleName("Transaction");

            add(MsoyUI.createLabel(MsoyUI.formatDateTime(entry.timestamp), "Time"));
            add(MsoyUI.createLabel(_lookup.xlate(entry.description), "Description"));

            String amt = String.valueOf(Math.abs(entry.amount));
            String debit, credit;
            if (entry.amount < 0) {
                debit = amt;
                credit = " ";
            } else {
                debit = " ";
                credit = amt;
            }
            add(MsoyUI.createLabel(debit, "Debit"));
            add(MsoyUI.createLabel(credit, "Credit"));
        }
    }

    protected static final MeMessagesLookup _lookup = GWT.create(MeMessagesLookup.class);
    protected static final MeMessages _msgs = GWT.create(MeMessages.class);
}
