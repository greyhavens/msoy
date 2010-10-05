//
// $Id$

package client.ui;

import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlineLabel;

/**
 * Creates a TabPanel that is styled by gwt.css to fit into our overall style.
 */
public class StyledTabPanel extends TabPanel
{
    // docs inherited
    public StyledTabPanel ()
    {
        super();
        setStyleName("StyledTabs");
        setWidth("100%");

        // This was turned off for a long time (it used to require a hack, and then for fear of
        // performance penalties) but we're turning it back on -- cause it really looks quite
        // nice -- and we'll see how it works in practice.
        getDeckPanel().setAnimationEnabled(true);
    }

    // docs inherited
    public void add (Widget w, String tabText)
    {
        this.add(w, tabText, false);
    }

    // docs inherited
    public void add (Widget w, String tabText, boolean asHTML)
    {
        Grid tab = new Grid (1, 3);
        tab.setCellSpacing(0);
        tab.setCellPadding(0);
        tab.getCellFormatter().setStyleName(0, 0, "TabLeft");
        tab.getCellFormatter().setStyleName(0, 1, "TabCenter");
        tab.getCellFormatter().setStyleName(0, 2, "TabRight");
        if (asHTML) {
            tab.setWidget(0, 1, MsoyUI.createHTML(tabText, null));
        } else {
            tab.setWidget(0, 1, new InlineLabel(tabText, false, false, false));
        }
        super.add(w, tab.toString(), true);
    }
}
