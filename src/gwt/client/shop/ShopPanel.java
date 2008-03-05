//
// $Id$

package client.shop;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.item.data.all.Item;

import client.shell.Application;
import client.shell.Page;
import client.util.MsoyUI;

/**
 * Displays the main catalog landing page.
 */
public class ShopPanel extends HorizontalPanel
{
    public ShopPanel ()
    {
        setStyleName("shopPanel");
        setVerticalAlignment(HasAlignment.ALIGN_TOP);

        add(new SideBar(Item.NOT_A_TYPE, null));
        add(WidgetUtil.makeShim(10, 10));

        // display a grid of the selectable item types
        VerticalPanel contents = new VerticalPanel();
        contents.add(MsoyUI.createLabel(CShop.msgs.catalogIntro(), "Intro"));
        SmartTable types = new SmartTable("Types", 0, 10);
        for (int ii = 0; ii < Item.TYPES.length; ii++) {
            byte type = Item.TYPES[ii];
            ClickListener onClick = Application.createLinkListener(Page.SHOP, ""+type);
            SmartTable ttable = new SmartTable("Type", 0, 2);
            String tpath = Item.getDefaultThumbnailMediaFor(type).getMediaPath();
            ttable.setWidget(0, 0, MsoyUI.createActionImage(tpath, onClick), 1, "Icon");
            ttable.getFlexCellFormatter().setRowSpan(0, 0, 2);
            String tname = CShop.dmsgs.getString("pItemType" + type);
            ttable.setWidget(0, 1, MsoyUI.createActionLabel(tname, onClick), 1, "Name");
            String tblurb = CShop.dmsgs.getString("catIntro" + type);
            ttable.setText(1, 0, tblurb, 1, "Blurb");
            types.setWidget(ii / 2, ii % 2, ttable);
        }
        contents.add(types);

        add(contents);
        setCellWidth(contents, "100%");
        add(WidgetUtil.makeShim(10, 10));
    }
}
