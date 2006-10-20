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
                "", path, THUMB_HEIGHT, THUMB_HEIGHT, null);

        case MediaDesc.IMAGE_PNG:
        case MediaDesc.IMAGE_JPEG:
        case MediaDesc.IMAGE_GIF:
            return new Image(path);

        default:
            return new Label(path);
        }
    }

    /** So arbitrary. TODO. */
    public static final int MAIN_HEIGHT = 200;

    /** So arbitrary. TODO. */
    public static final int FURNI_HEIGHT = 100;

    /** So arbitrary. TODO. */
    public static final int THUMB_HEIGHT = 100;

    public ItemContainer (ItemPanel panel, Item item)
    {
        this(panel, item, true, true);
    }

    public ItemContainer (ItemPanel panel, Item item, boolean thumbnail,
                          boolean showLabel)
    {
        _panel = panel;
        setItem(item, thumbnail, showLabel);
    }

    public void setItem (Item item, boolean thumbnail, boolean showLabel)
    {
        if (item == null) {
            return;
        }
        _item = item;

        // clear out our old UI, and we'll create it anew
        clear();

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

        if (showLabel) {
            add(label);
        }
        add(disp);

        HorizontalPanel buttons = new HorizontalPanel();
        buttons.setSpacing(5);
        add(buttons);

        Button button;
        if (item.parentId == -1) {
            button = new Button("List in Catalog ...");
            button.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    _panel.listItem(_item.getIdent());
                }
            });

        } else {
            button = new Button("Remix ...");
            button.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    _panel.remixItem(_item.getIdent());
                }
            });
        }
        buttons.add(button);

        // TODO: all these buttons have to go soon
        button = new Button("Details ...");
        button.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                new ItemView(_panel._ctx, _item).show();
            }
        });
        buttons.add(button);

        if (item.parentId == -1) {
            button = new Button("Edit ...");
            button.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    ItemEditor editor =
                        _panel.createItemEditor(_item.getType());
                    editor.setItem(_item);
                    editor.show();
                }
            });
            buttons.add(button);
        }
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
