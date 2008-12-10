//
// $Id$

package client.me;

import java.util.List;

import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.money.data.all.MoneyTransaction;

import client.ui.MsoyUI;

public class BalancePanel extends MoneyPanel
{
    public BalancePanel (MoneyTransactionDataModel model, Widget controller)
    {
        super(model, controller);
    }

    @Override
    protected void addCustomRow (MoneyTransaction entry, List<Widget> row)
    {
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
        row.add(MsoyUI.createLabel(entry.currency.format(entry.balance), "Balance"));
    }

    @Override
    protected void addCustomHeader (List<Widget> header)
    {
        header.add(MsoyUI.createLabel(_msgs.reportColumnDebit(), null));
        header.add(MsoyUI.createLabel(_msgs.reportColumnCredit(), null));
        header.add(MsoyUI.createLabel(_msgs.reportColumnBalance(), null));
    }
}
