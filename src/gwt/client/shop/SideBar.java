//
// $Id$

package client.shop;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.all.Item;

import com.threerings.gwt.ui.SmartTable;

import client.shell.DynamicMessages;
import client.shell.Pages;
import client.ui.MsoyUI;
import client.util.Link;

/**
 * Shown next to our catalog listings and our catalog landing page.
 */
public class SideBar extends SmartTable
{
    public interface Linker {
        public boolean isSelected (byte itemType);
        public String composeArgs (byte itemType);
    }

    public SideBar (Linker linker, boolean showAll, Widget extras)
    {
        super("sideBar", 0, 0);

        addWidget(new Image("/images/shop/sidebar_top.png"), 1, null);
        addText(_msgs.catalogCats(), 1, "Title");

        FlowPanel navi = new FlowPanel();
        navi.setStyleName("NaviPanel");
        if (showAll) {
            navi.add(makeItem(linker, _msgs.allItems(), Item.NOT_A_TYPE));
        }
        for (int ii = 0; ii < Item.TYPES.length; ii++) {
            byte type = Item.TYPES[ii];
            navi.add(makeItem(linker, _dmsgs.getString("pItemType" + type), type));
        }
        addWidget(navi, 1, "Middle");

        if (extras != null) {
            addWidget(extras, 1, "Middle");
        }
        addWidget(new Image("/images/shop/sidebar_bottom.png"), 1, null);
    }

    protected Widget makeItem (Linker linker, String name, byte itemType)
    {
        if (linker.isSelected(itemType)) {
            return MsoyUI.createLabel(name, "Selected");
        } else {
            Widget link = Link.create(name, Pages.SHOP, linker.composeArgs(itemType));
            link.removeStyleName("inline");
            return link;
        }
    }

    protected static final ShopMessages _msgs = GWT.create(ShopMessages.class);
    protected static final DynamicMessages _dmsgs = GWT.create(DynamicMessages.class);
}
