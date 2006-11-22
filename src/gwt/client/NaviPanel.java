//
// $Id$

package client;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;

import com.threerings.msoy.web.client.WebContext;

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
            String href = "<a href=\"" + PAGES[ii] + ".html\">" + PAGES[ii+1] + "</a>";
            setWidget(0, column++, new HTML(href));
            getFlexCellFormatter().setStyleName(0, column++, prefix + "Right");
        }
    }

    // TODO: translate
    protected static final String[] PAGES = {
        "index", "World",
        "person", "Page",
        "group", "Groups",
        "mail", "Mail",
        "inventory", "Inventory",
        "catalog", "Catalog",
    };
}

//       <table class="Links" cellspacing=0 cellpadding=0><tr>
//       <td class="Left"></td><td class="Link"><a href="index.html">World</a></td><td class="Right"></td>
//       <td class="SelLeft"></td><td class="SelLink"><a href="person.html">Page</a></td><td class="SelRight"></td>
//       <td class="Left"></td><td class="Link"><a href="group.html">Groups</a></td><td class="Right"></td>
//       <td class="Left"></td><td class="Link"><a href="mail.html">Mail</a></td><td class="Right"></td>
//       <td class="Left"></td><td class="Link"><a href="inventory.html">Inventory</a></td><td class="Right"></td>
//       <td class="Left"></td><td class="Link"><a href="catalog.html">Catalog</a></td><td class="Right"></td>
//       </tr></table>
