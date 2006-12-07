//
// $Id$

package client.inventory;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.web.Item;

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

        add(createContainer(item));

        Label label = new Label(truncateDescription(item.getDescription()));
        label.setStyleName("itemThumbText");
        label.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                new ItemView(_panel._ctx, _item, _panel).show();
            }
        });
        add(label);
    }

    /**
     * Helper method to create the container widget.
     */
    protected Widget createContainer (Item item)
    {
        return ItemUtil.createMediaView(item.getThumbnailMedia(), true);
    }

    /**
     * Convenience method to truncate the specified description to fit.
     */
    protected String truncateDescription (String text)
    {
        return (text.length() <= 32) ? text : (text.substring(0, 29) + "...");
    }

    protected ItemPanel _panel;
    protected Item _item;
}
