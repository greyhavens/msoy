//
// $Id$

package client.inventory;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.MediaDesc;
import com.threerings.msoy.item.web.Photo;

import client.MsoyEntryPoint;

import client.util.FlashWidget;

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
    /** So arbitrary. TODO. */
    public static final int THUMB_HEIGHT = 100;

    public ItemContainer (Item item)
    {
        this(item, true, true);
    }

    public ItemContainer (Item item, boolean thumbnail, boolean showLabel)
    {
/*
        setItem(item, thumbnail, showLabel);
    }

    public void setItem (Item item, boolean thumbnail, boolean showLabel)
    {
        while (getWidgetCount() > 0) {
            remove(0);
        }

        if (item == null) {
            return;
        }
*/

        Widget disp = createContainer(item);
        Label label = null;
        if (showLabel) {
            label = new Label(truncateDescription(item.getDescription()));
        }

        if (thumbnail) {
            disp.setStyleName("item_thumb_image");
            disp.setHeight(THUMB_HEIGHT + "px");
            label.setStyleName("item_thumb_text");

        } else {
            // TODO: sort this out, setting a style name on the FlashWidget
            // here seems to freak it out, but it works at other times.
            /*
            disp.setStyleName("item_image");
            disp.setPixelSize(THUMB_WIDTH, THUMB_HEIGHT);
            label.setStyleName("item_text");
            */
        }

        add(disp);
        if (showLabel) {
            add(label);
        }
    }

    /**
     * Helper method to create the container widget.
     */
    protected Widget createContainer (Item item)
    {
        String thumbPath = MsoyEntryPoint.toMediaPath(item.getThumbnailPath());
        switch (MediaDesc.suffixToMimeType(thumbPath)) {
        case MediaDesc.APPLICATION_SHOCKWAVE_FLASH:
            String ident = String.valueOf(item.itemId);
            FlashWidget fw = new FlashWidget(ident);
            fw.setMovie(thumbPath);
            return fw;

        default:
            return new Image(thumbPath);
        }
    }

    /**
     * Convenience method to truncate the specified description to fit.
     */
    protected String truncateDescription (String text)
    {
        return (text.length() <= 32) ? text : (text.substring(0, 29) + "...");
    }
}
