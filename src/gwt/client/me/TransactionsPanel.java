//
// $Id$

package client.me;

import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.msoy.money.data.all.Currency;

import client.ui.MsoyUI;

public class TransactionsPanel extends VerticalPanel
{
    public TransactionsPanel (int memberId)
    {
        //setStyleName("mePanel");

        // TODO: Fixer upper
        add(MsoyUI.createLabel("Bars", ""));
        add(new BalancePanel(memberId, Currency.BARS));
        add(MsoyUI.createLabel("Coins", ""));
        add(new BalancePanel(memberId, Currency.COINS));
    }
}
