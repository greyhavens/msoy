//
// $Id$

package client.me;

import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.msoy.money.data.all.ReportType;

import client.ui.MsoyUI;

public class TransactionsPanel extends VerticalPanel
{
    public TransactionsPanel (int memberId)
    {
        // TODO: Fixer upper
        add(MsoyUI.createLabel("Bars", ""));
        add(new BalancePanel(memberId, ReportType.BARS));
        add(MsoyUI.createLabel("Coins", ""));
        add(new BalancePanel(memberId, ReportType.COINS));
        add(MsoyUI.createLabel("Creator", ""));
        add(new BalancePanel(memberId, ReportType.CREATOR));
    }
}
