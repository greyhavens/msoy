//
// $Id$

package client.inventory;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.MediaDesc;

import client.item.ItemUtil;

/**
 * Displays a thumbnail version of an item.
 *
 * <p> Styles:
 * <ul>
 * <li> item_image - the style of a full-size item image
 * <li> item_text - the style for "full-size" text
 * <li> item_thumb_image - the style of item thumbnail image
 * <li> item_thumb_text - the style of item thumbnail text
 * </ul>
 */
public class ItemContainer extends VerticalPanel
{
    public ItemContainer (ItemPanel panel, Item item)
    {
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

        SimplePanel wrapper = new SimplePanel();
        wrapper.setStyleName("Preview");
        wrapper.add(createContainer(item));
        add(wrapper);

        Label label = new Label(ItemUtil.getName(_panel._ctx, item, true));
        label.setStyleName("ThumbText");
        label.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                new ItemDetailPopup(_panel._ctx, _item, _panel).show();
            }
        });
        add(label);
    }

    /**
     * Helper method to create the container widget.
     */
    protected Widget createContainer (Item item)
    {
        return ItemUtil.createMediaView(item.getThumbnailMedia(), MediaDesc.THUMBNAIL_SIZE);
    }

    protected ItemPanel _panel;
    protected Item _item;
}
