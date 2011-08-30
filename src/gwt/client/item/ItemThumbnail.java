//
// $Id$

package client.item;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlexTable;

import com.threerings.orth.data.MediaDescSize;

import com.threerings.msoy.item.data.all.Item;

import client.ui.MsoyUI;
import client.util.MediaUtil;

/**
 * A little widget to carry a thumbnail and a label.
 */
public class ItemThumbnail extends FlexTable
{
    public ItemThumbnail (Item item, ClickHandler listener)
    {
        _item = item;

        setStyleName("itemThumbnail");
        setCellPadding(0);
        setCellSpacing(0);
        setWidget(0, 0, MediaUtil.createMediaView(
                      item.getThumbnailMedia(), MediaDescSize.THUMBNAIL_SIZE, listener));
        if (listener != null) {
            setWidget(1, 0, MsoyUI.createActionLabel(ItemUtil.getName(item), "Text", listener));
        } else {
            setWidget(1, 0, MsoyUI.createLabel(ItemUtil.getName(item), "Text"));
        }
    }

    public Item getItem ()
    {
        return _item;
    }

    protected Item _item;
}
