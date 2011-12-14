//
// $Id$

package client.me;

import java.util.List;

import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.money.data.all.MoneyTransaction;

import client.shell.CShell;
import client.ui.MsoyUI;
import client.util.Link;

public class IncomePanel extends MoneyPanel
{
    public IncomePanel (MoneyTransactionDataModel model, Widget controller)
    {
        super(model, controller);
    }

    @Override
    protected void addCustomRow (MoneyTransaction entry, List<Widget> row)
    {
        // Pack the currency icon and amount into one column
        HorizontalPanel income = new HorizontalPanel();
        income.add(MsoyUI.createInlineImage(entry.currency.getSmallIcon()));
        income.add(WidgetUtil.makeShim(15, 1));
        income.add(MsoyUI.createLabel(entry.currency.format(entry.amount), "Income"));

        row.add(income);

        if (CShell.isSupport() && entry.referenceMemberName != null) {
            row.add(Link.memberView(entry.referenceMemberName));
        }
    }

    @Override
    protected void addCustomHeader (List<Widget> header)
    {
        header.add(MsoyUI.createLabel(_msgs.reportColumnIncome(), null));
        if (CShell.isSupport()) {
            header.add(MsoyUI.createLabel(_msgs.reportColumnBuyer(), null));
        }
    }
}
