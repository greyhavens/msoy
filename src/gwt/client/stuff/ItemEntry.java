//
// $Id$

package client.stuff;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;

import client.item.ItemActivator;
import client.util.FlashClients;
import client.util.ItemUtil;
import client.util.MediaUtil;
import client.util.MsoyUI;

/**
 * Displays a thumbnail version of an item.
 */
public class ItemEntry extends FlexTable
{
    public ItemEntry (Item item)
    {
        setCellPadding(0);
        setCellSpacing(0);
        setStyleName("itemEntry");
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

        ClickListener clicker = new ClickListener() {
            public void onClick (Widget sender) {
                CStuff.viewItem(_item.getType(), _item.itemId);
            }
        };
        setWidget(0, 0, MediaUtil.createMediaView(
                      item.getThumbnailMedia(), MediaDesc.THUMBNAIL_SIZE, clicker));
        getFlexCellFormatter().setStyleName(0, 0, "Preview");

        setWidget(0, 1, MsoyUI.createActionLabel(ItemUtil.getName(item, true), clicker));
        getFlexCellFormatter().setStyleName(0, 1, "ThumbText");
        if (_item.itemId > 0) { // if this item is an original, style it slightly differently
            getFlexCellFormatter().addStyleName(0, 1, "OriginalThumbText");
        }

        if (FlashClients.clientExists()) {
            setWidget(1, 0, new ItemActivator(_item));
            getFlexCellFormatter().setRowSpan(0, 0, 2);
        }
    }

    protected Item _item;
}
