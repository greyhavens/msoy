//
// $Id$

package client.me;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedGrid;

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

    @Override
    public Widget createWidget (MoneyTransaction entry)
    {
        return new TransactionWidget(entry);
    }

    @Override
    public String getEmptyMessage ()
    {
        return _msgs.transactionsNone();
    }

    @Override
    protected boolean displayNavi (int items)
    {
        return true;
    }

    protected static class TransactionWidget extends HorizontalPanel
    {
        public TransactionWidget (MoneyTransaction entry)
        {
            addStyleName("Transaction");

            add(MsoyUI.createLabel(MsoyUI.formatDateTime(entry.timestamp), "Time"));

            String description = _lookup.xlate(MsoyUI.escapeHTML(entry.description));
            add(MsoyUI.createHTML(description, "Description"));

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
