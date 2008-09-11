//
// $Id$

package client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.gwt.ui.InlineLabel;

import com.threerings.msoy.money.data.all.Currency;

import client.shell.ShellMessages;

/**
 * Displays the cost of an item.
 */
public class PriceLabel extends FlowPanel
{
    public PriceLabel (Currency currency, int cost)
    {
        setStyleName("Price");
        updatePrice(currency, cost);
    }

    public void updatePrice (Currency currency, int cost)
    {
        clear();

        add(new InlineLabel(_cmsgs.price(), false, false, true));
        add(MsoyUI.createInlineImage(currency.getSmallIcon()));
        add(new InlineLabel(currency.format(cost), false, false, true));
    }

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
}
