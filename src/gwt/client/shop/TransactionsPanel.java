//
// $Id$

package client.shop;

import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.msoy.money.data.all.MoneyType;

import client.ui.MsoyUI;

public class TransactionsPanel extends VerticalPanel
{
    public TransactionsPanel (int memberId)
    {
        setStyleName("shopPanel");

        // TODO: Fixer upper
        add(MsoyUI.createLabel("Bars", ""));
        add(new BalancePanel(memberId, MoneyType.BARS));
        add(MsoyUI.createLabel("Coins", ""));
        add(new BalancePanel(memberId, MoneyType.COINS));
    }
}
