//
// $Id$

package client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;

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

    public static Image createIcon (Currency currency)
    {
        // TODO: Handle bling?
        String path = "/images/ui/" + (currency == Currency.COINS ? "coins.png" : "gold.png");
        return MsoyUI.createInlineImage(path);
    }

    public void updatePrice (Currency currency, int cost)
    {
        clear();

        add(new InlineLabel(_cmsgs.price(), false, false, true));
        add(createIcon(currency));
        add(new InlineLabel(""+cost, false, false, true));
    }

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
}
