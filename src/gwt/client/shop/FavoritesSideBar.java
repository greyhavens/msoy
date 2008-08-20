//
// $Id$

package client.shop;

import java.util.ArrayList;
import java.util.List;

import client.shell.Args;
import client.shell.DynamicMessages;
import client.shell.Pages;
import client.ui.MsoyUI;
import client.util.Link;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.msoy.item.data.all.Item;

/**
 * A side menu for filtering which type of favorite items to display.
 *
 * @author mjensen
 */
public class FavoritesSideBar extends SmartTable
{
    public FavoritesSideBar ()
    {
        super("sideBar", 0, 0);
    }

    public void update (String[] prefixArgs, byte selectedItemType)
    {
        // remove all of the widgets and add fresh ones
        clear();
        addWidget(new Image("/images/shop/sidebar_top.png"), 1, null);
        addWidget(MsoyUI.createLabel(CShop.msgs.favoritesSideBarTitle(), "Title"), 1, null);
        addWidget(new NaviPanel(prefixArgs, selectedItemType), 1, "Middle");
        addWidget(new Image("/images/shop/sidebar_bottom.png"), 1, null);
    }

    protected static class NaviPanel extends FlowPanel
    {
        public NaviPanel (String[] prefixArgs, byte selectedType) {
            addStyleName("NaviPanel");
            addStyleName("FavoritesNaviPanel");
            // add link for "All" items
            add(createMenuItem(prefixArgs, selectedType, CShop.msgs.allItems(), Item.NOT_A_TYPE));
            for (int ii = 0; ii < Item.TYPES.length; ii++) {
                byte type = Item.TYPES[ii];
                String name = _dmsgs.getString("pItemType" + type);
                add(createMenuItem(prefixArgs, selectedType, name, type));
            }
        }

        protected static Widget createMenuItem (String[] prefixArgs, byte selectedType,
            String name, byte type)
        {
            if (selectedType == type) {
                return MsoyUI.createLabel(name, "Selected");
            }

            Widget link = Link.create(name, Pages.SHOP, toLinkArguments(prefixArgs, type));
            link.removeStyleName("inline");
            return link;
        }

        /** Makes a search argument string, preserving constraints. */
        protected static String toLinkArguments (String[] prefixArgs, byte newType)
        {
            // the last argument indicates the item type
            List<String> args = new ArrayList<String>();
            for (String arg : prefixArgs) {
                args.add(arg);
            }
            args.add(String.valueOf(newType));
            return Args.compose(args);
        }
    }

    protected static final DynamicMessages _dmsgs = GWT.create(DynamicMessages.class);
}
