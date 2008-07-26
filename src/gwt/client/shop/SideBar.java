//
// $Id$

package client.shop;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.web.data.CatalogQuery;

import com.threerings.gwt.ui.SmartTable;

import client.shell.DynamicMessages;
import client.shell.Page;
import client.util.Link;
import client.util.MsoyUI;
import client.util.ShopUtil;

/**
 * Shown next to our catalog listings and our catalog landing page.
 */
public class SideBar extends SmartTable
{
    public SideBar (CatalogQuery query, Widget extras)
    {
        super("sideBar", 0, 0);

        addWidget(new Image("/images/shop/sidebar_top.png"), 1, null);
        addText(CShop.msgs.catalogCats(), 1, "Title");
        addWidget(new NaviPanel(query), 1, "Middle");
        if (extras != null) {
            addWidget(extras, 1, "Middle");
        }
        addWidget(new Image("/images/shop/sidebar_bottom.png"), 1, null);
    }

    protected static class NaviPanel extends FlowPanel
    {
        public NaviPanel (CatalogQuery query) {
            setStyleName("NaviPanel");
            for (int ii = 0; ii < Item.TYPES.length; ii++) {
                byte type = Item.TYPES[ii];
                String name = _dmsgs.getString("pItemType" + type);

                if (query.itemType == type) {
                    add(MsoyUI.createLabel(name, "Selected"));
                } else {
                    Widget link = Link.create(
                        name, Page.SHOP, toLinkArguments(query, type, 0));
                    link.removeStyleName("inline");
                    add(link);
                }
            }
        }

        /** Makes a search argument string, preserving constraints. */
        protected String toLinkArguments (CatalogQuery query, byte newtype, int page)
        {
            CatalogQuery copy = new CatalogQuery(query);
            copy.itemType = newtype;
            return ShopUtil.composeArgs(copy, page);
        }
    }

    protected static final DynamicMessages _dmsgs = GWT.create(DynamicMessages.class);
}
