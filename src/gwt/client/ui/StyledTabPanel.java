//
// $Id$

package client.ui;

import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.SourcesTabEvents;

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

        // This animation is actually pretty damn slick, but given the bug we have to work around
        // below, I don't trust it to work on various browsers, etc.  Plus, who knows how it
        // performs on something other than FF3
        getDeckPanel().setAnimationEnabled(false);
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
            tab.setWidget(0, 1, new HTML(tabText));
        } else {
            tab.setWidget(0, 1, new InlineLabel(tabText, false, false, false));
        }
        super.add(w, tab.toString(), true);
    }

    @Override
    public void onTabSelected (SourcesTabEvents sender, int tabIndex)
    {
        super.onTabSelected(sender, tabIndex);

        // GWT 1.5.0 introduced a animation class in DeckPanel that doesn't seem to correctly
        // call setVisible on the newly added Widget, which our widgets depend on.  Glancing at the
        // code, it's not entirely clear why it doesn't get called, but to be sure it does not,
        // unless we do it here.
        DeckPanel deck = getDeckPanel();
        // for some reason, getVisibleWidget returns the index, rather than the widget itself
        deck.getWidget(deck.getVisibleWidget()).setVisible(true);
    }
}
