//
// $Id$

package client.inventory;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.MediaDesc;

import client.util.ItemUtil;
import client.util.MediaUtil;

/**
 * Displays a thumbnail version of an item.
 */
public class ItemContainer extends FlexTable
{
    public ItemContainer (ItemPanel panel, Item item)
    {
        setCellPadding(0);
        setCellSpacing(0);
        _panel = panel;
        setStyleName("itemContainer");
        setItem(item);
    }

    public void setItem (Item item)
    {
        if (item == null) {
            return;
        }
        _item = item;

        // clear out our old UI, and we'll create it anew
        clear();

        Widget mview = MediaUtil.createMediaView(
            item.getThumbnailMedia(), MediaDesc.THUMBNAIL_SIZE);
        if (mview instanceof Image) {
            ((Image)mview).addClickListener(_clicker);
            mview.addStyleName("actionLabel");
        }
        setWidget(0, 0, mview);
        getFlexCellFormatter().setStyleName(0, 0, "Preview");

        Label label = new Label(ItemUtil.getName(item, true));
        label.setStyleName("ThumbText");
        label.addClickListener(_clicker);
        setWidget(1, 0, label);
    }

    protected ClickListener _clicker = new ClickListener() {
        public void onClick (Widget sender) {
            CInventory.log("Getting info on " + _item.itemId);
            _panel.requestShowDetail(_item.itemId);
        }
    };

    protected ItemPanel _panel;
    protected Item _item;
}
