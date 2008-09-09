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

        if (showAll) {
            init(linker, ALL_TYPES, extras);
        } else {
            init(linker, Item.TYPES, extras);
        }
    }

    public SideBar (Linker linker, byte[] itemTypes, Widget extras)
    {
        super("sideBar", 0, 0);
        init(linker, itemTypes, extras);
    }

    protected void init (Linker linker, byte[] itemTypes, Widget extras)
    {
        addWidget(new Image("/images/shop/sidebar_top.png"), 1, null);
        addText(_msgs.catalogCats(), 1, "Title");

        FlowPanel navi = new FlowPanel();
        navi.setStyleName("NaviPanel");
        navi.add(new Image("/images/shop/navi_bg_top.png"));
        for (int ii = 0; ii < itemTypes.length; ii++) {
            byte type = itemTypes[ii];
            if (ii > 0) {
                navi.add(new Image("/images/shop/navi_bg_sep.png"));
            }
            navi.add(makeItem(linker, _dmsgs.getString("pItemType" + type), type));
        }
        navi.add(new Image("/images/shop/navi_bg_bottom.png"));
        addWidget(navi, 1, "Middle");

        if (extras != null) {
            addWidget(extras, 1, "Middle");
        }
        addWidget(new Image("/images/shop/sidebar_bottom.png"), 1, null);
    }

    protected Widget makeItem (Linker linker, String name, byte itemType)
    {
        Widget itemWidget;
        if (linker.isSelected(itemType)) {
            itemWidget = MsoyUI.createLabel(name, "Selected");
        } else {
            Widget link = Link.create(name, Pages.SHOP, linker.composeArgs(itemType));
            link.removeStyleName("inline");
            itemWidget = link;
        }
        itemWidget.addStyleName("Cell");
        if (name.length() > 10) {
            itemWidget.addStyleName("LongCell");
        }
        return itemWidget;
    }

    protected static final byte[] ALL_TYPES = new byte[] { Item.NOT_A_TYPE, Item.AVATAR,
            Item.FURNITURE, Item.DECOR, Item.TOY, Item.PET, Item.GAME, Item.PHOTO, Item.AUDIO,
            Item.VIDEO };

    protected static final ShopMessages _msgs = GWT.create(ShopMessages.class);
    protected static final DynamicMessages _dmsgs = GWT.create(DynamicMessages.class);
}
