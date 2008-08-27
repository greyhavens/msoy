//
// $Id$

package client.shop;

import com.google.gwt.user.client.ui.VerticalPanel;

import client.ui.MsoyUI;

public class TransactionsPanel extends VerticalPanel
{
    public TransactionsPanel (int memberId)
    {
        setStyleName("shopPanel");

        add(MsoyUI.createLabel("Transaction History WIP:", ""));
        add(new BalancePanel(memberId));
    }
}
