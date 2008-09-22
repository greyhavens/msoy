//
// $Id$

package client.me;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedTable;

import com.threerings.msoy.money.data.all.MoneyTransaction;
import com.threerings.msoy.money.data.all.ReportType;

import client.ui.MsoyUI;

public class BalancePanel extends PagedTable<MoneyTransaction>
{
    public BalancePanel (MoneyTransactionDataModel model)
    {
        super(10, NAV_ON_TOP);

        addStyleName("Balance");

        _report = model.report;
        setModel(model, 0);
    }

    @Override
    public List<Widget> createRow (MoneyTransaction entry)
    {
        List<Widget> row = new ArrayList<Widget>();

        row.add(MsoyUI.createLabel(MsoyUI.formatDateTime(entry.timestamp), "Time"));

        String description = _lookup.xlate(MsoyUI.escapeHTML(entry.description));
        row.add(MsoyUI.createHTML(description, "Description"));

        if (_report == ReportType.CREATOR) {
            row.add(MsoyUI.createInlineImage(entry.currency.getSmallIcon()));
            row.add(MsoyUI.createLabel(entry.currency.format(entry.amount), ""));
        } else {
            String amt = entry.currency.format(Math.abs(entry.amount));
            String debit, credit;
            if (entry.amount < 0) {
                debit = amt;
                credit = " ";
            } else {
                debit = " ";
                credit = amt;
            }
            row.add(MsoyUI.createLabel(debit, "Debit"));
            row.add(MsoyUI.createLabel(credit, "Credit"));
        }

        return row;
    }

    @Override
    public List<Widget> createHeader ()
    {
        List<Widget> header = new ArrayList<Widget>();

        header.add(MsoyUI.createLabel("When", null));
        header.add(MsoyUI.createLabel("How", null));
        header.add(MsoyUI.createLabel("Debit", null));
        header.add(MsoyUI.createLabel("Credit", null));

        return header;
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

    protected ReportType _report;

    protected static final MeMessagesLookup _lookup = GWT.create(MeMessagesLookup.class);
    protected static final MeMessages _msgs = GWT.create(MeMessages.class);
}
