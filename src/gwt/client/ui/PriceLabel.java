//
// $Id$

package client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.gwt.ui.InlineLabel;

import client.shell.ShellMessages;

/**
 * Displays the cost of an item.
 */
public class PriceLabel extends FlowPanel
{
    public PriceLabel (int flowCost, int goldCost)
    {
        setStyleName("Price");
        updatePrice(flowCost, goldCost);
    }

    public void updatePrice (int flowCost, int goldCost)
    {
        clear();
        add(new InlineLabel(_cmsgs.price(), false, false, true));
        if (goldCost > 0) {
            add(MsoyUI.createInlineImage("/images/ui/gold.png"));
            add(new InlineLabel(""+goldCost, false, false, true));
        }
        if (flowCost > 0 || (goldCost == 0)) {
            add(MsoyUI.createInlineImage("/images/ui/coins.png"));
            add(new InlineLabel(""+flowCost, false, false, true));
        }
    }

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
}
