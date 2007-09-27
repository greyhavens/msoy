//
// $Id$

package client.util;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;

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

        // the thumbnail is always an image, so we can cast safely here
        Image image = (Image) MediaUtil.createMediaView(
            item.getThumbnailMedia(), MediaDesc.HALF_THUMBNAIL_SIZE);
        image.addStyleName("Image");
        image.addClickListener(listener);
        setWidget(0, 0, image);

        Label label = new Label(ItemUtil.getName(item));
        label.addStyleName("Text");
        label.addClickListener(listener);
        setWidget(1, 0, label);
    }

    public Item getItem ()
    {
        return _item;
    }

    protected Item _item;
}
