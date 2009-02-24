//
// $Id$

package client.ui;

import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.gwt.ui.InlineLabel;

import com.threerings.msoy.money.data.all.Currency;

/**
 * Displays a currency icon and a quantity of money.
 */
public class MoneyLabel extends FlowPanel
{
    public MoneyLabel (Currency currency, int cost)
    {
        setStyleName("moneyLabel");
        update(currency, cost);
    }

    public void update (Currency currency, int cost)
    {
        clear();
        createUI(currency, cost);
    }

    protected void createUI  (Currency currency, int cost)
    {
        add(MsoyUI.createInlineImage(currency.getSmallIcon()));
        add(new InlineLabel(currency.format(cost), false, true, false));
    }
}
