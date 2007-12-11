//
// $Id$

package client.util;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;

/**
 * A little widget to carry a thumbnail and a label; a light-weight {@link ItemContainer} perhaps.
 */
public class ItemThumbnail extends FlexTable
{
    public ItemThumbnail (Item item, ClickListener listener)
    {
        _item = item;

        setStyleName("itemThumbnail");
        setCellPadding(0);
        setCellSpacing(0);
        setWidget(0, 0, MediaUtil.createMediaView(
                      item.getThumbnailMedia(), MediaDesc.THUMBNAIL_SIZE, listener));
        setWidget(1, 0, MsoyUI.createActionLabel(ItemUtil.getName(item), "Text", listener));
    }

    public Item getItem ()
    {
        return _item;
    }

    protected Item _item;
}
