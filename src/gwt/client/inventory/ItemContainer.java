//
// $Id$

package client.inventory;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.MediaDesc;

import client.shell.MsoyEntryPoint;

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
    /**
     * Create a container to hold the media in the specified path.
     */
    public static Widget createContainer (String path)
    {
        switch (MediaDesc.suffixToMimeType(path)) {
        case MediaDesc.APPLICATION_SHOCKWAVE_FLASH:
            return WidgetUtil.createFlashContainer(
                "", path, Item.THUMBNAIL_WIDTH, Item.THUMBNAIL_HEIGHT, null);

        case MediaDesc.IMAGE_PNG:
        case MediaDesc.IMAGE_JPEG:
        case MediaDesc.IMAGE_GIF:
            return new Image(path);

        default:
            return new Label(path);
        }
    }

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

        Widget disp = createContainer(item);
        disp.setStyleName("itemThumbImage");
        add(disp);

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
        String thumbPath = MsoyEntryPoint.toMediaPath(item.getThumbnailPath());
        return createContainer(thumbPath);
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
