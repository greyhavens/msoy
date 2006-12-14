//
// $Id$

package client.shell;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;

import client.util.WebContext;

/**
 * Displays our navigation headers.
 */
public class NaviPanel extends FlexTable
{
    public NaviPanel (WebContext ctx, String page)
    {
        setStyleName("naviPanel");
        setCellPadding(0);
        setCellSpacing(0);

        int column = 0;
        for (int ii = 0; ii < PAGES.length; ii += 2) {
            String prefix = PAGES[ii].equals(page) ? "Sel" : "";
            getFlexCellFormatter().setStyleName(0, column++, prefix + "Left");
            getFlexCellFormatter().setStyleName(0, column, prefix + "Link");
            String href = "<a href=\"/" + PAGES[ii] + "/index.html\">" + PAGES[ii+1] + "</a>";
            setWidget(0, column++, new HTML(href));
            getFlexCellFormatter().setStyleName(0, column++, prefix + "Right");
        }
    }

    // TODO: translate
    protected static final String[] PAGES = {
        "world", "World",
        "profile", "Profile",
        "group", "Groups",
        "mail", "Mail",
        "inventory", "Inventory",
        "catalog", "Catalog",
    };
}
