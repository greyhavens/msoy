//
// $Id$

package client.inventory;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.MediaDesc;

import client.MsoyEntryPoint;

import client.util.WidgetUtil;

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
            return WidgetUtil.createFlashMovie(
                // TODO: allow passing -1 for width
                "", path, THUMB_HEIGHT, THUMB_HEIGHT);

        default:
            return new Image(path);
        }
    }

    /** So arbitrary. TODO. */
    public static final int THUMB_HEIGHT = 100;

    public ItemContainer (Item item, ItemPanel panel)
    {
        this(item, panel, true, true);
    }

    public ItemContainer (
            final Item item, final ItemPanel panel,
            boolean thumbnail, boolean showLabel)
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
        if (item.parentId == -1) {
            Button button = new Button("List in Catalog ...");
            button.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    panel.listItem(item.getIdent());
                }
            });
            add(button);
        } else {
            Button button = new Button("Remix ...");
            button.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    panel.remixItem(item.getIdent());
                }
            });
            add(button);            
        }
        // TODO: all these buttons have to go soon
        Button button = new Button("Details ...");
        button.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                ItemDetail foo = new ItemDetail(panel._ctx, item);
                foo.setPopupPosition(
                    sender.getAbsoluteLeft()+20, sender.getAbsoluteTop()-200);
                foo.show();
            }
        });
        add(button);            

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
}
