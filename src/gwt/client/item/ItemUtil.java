//
// $Id$

package client.item;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.MediaDesc;

import com.threerings.gwt.ui.WidgetUtil;

import client.shell.MsoyEntryPoint;
import client.util.WebContext;

/**
 * Contains utility methods for item related user interface business.
 */
public class ItemUtil
{
    /**
     * Returns the name of this item or a properly translated string indicating that it has no
     * name.
     */
    public static String getName (WebContext ctx, Item item)
    {
        return getName(ctx, item, false);
    }

    /**
     * Returns the truncated name of this item or a properly translated string indicating that it
     * has no name.
     */
    public static String getName (WebContext ctx, Item item, boolean truncate)
    {
        String name = (item.name.trim().length() == 0) ? "<no name>" : item.name;
        if (name.length() > 32 && truncate) {
            name = name.substring(0, 29) + "...";
        }
        return name;
    }

    /**
     * Returns the description of this item or a properly translated string indicating that it has
     * no description.
     */
    public static String getDescription (WebContext ctx, Item item)
    {
        return (item.description.trim().length() == 0) ?
            "No description provided for this item." : item.description;
    }

    /**
     * Create a widget to display the supplied media.
     *
     * @param size either {@link MediaDesc#HALF_THUMBNAIL_SIZE}, {@link MediaDesc#THUMBNAIL_SIZE}
     * or {@link MediaDesc#PREVIEW_SIZE}.
     */
    public static Widget createMediaView (MediaDesc desc, int size)
    {
        return createMediaView(desc, MediaDesc.DIMENSIONS[2*size], MediaDesc.DIMENSIONS[2*size+1]);
    }

    /**
     * Create a widget to display the supplied media. The media will be configured to scale
     * properly to constraint it to the indicated size. The supplied target width and height which
     * must be in the same ratio as the ratio between {@link Item#THUMBNAIL_WIDTH} and {@link
     * Item#THUMBNAIL_HEIGHT}.
     */
    public static Widget createMediaView (MediaDesc desc, int width, int height)
    {
        String path = MsoyEntryPoint.toMediaPath(desc.getMediaPath());
        Widget view;

        switch (MediaDesc.suffixToMimeType(path)) {
        case MediaDesc.APPLICATION_SHOCKWAVE_FLASH:
            view = WidgetUtil.createFlashContainer("", path, width, height, null);
            break;

        case MediaDesc.IMAGE_PNG:
        case MediaDesc.IMAGE_JPEG:
        case MediaDesc.IMAGE_GIF:
            view = new Image(path);
            switch (desc.constraint) {
            case MediaDesc.HALF_HORIZONTALLY_CONSTRAINED:
                if (width < MediaDesc.THUMBNAIL_WIDTH) {
                    view.setWidth(width + "px");
                }
                break;
            case MediaDesc.HALF_VERTICALLY_CONSTRAINED:
                if (height < MediaDesc.THUMBNAIL_HEIGHT) {
                    view.setHeight(height + "px");
                }
                break;
            case MediaDesc.HORIZONTALLY_CONSTRAINED:
                view.setWidth(width + "px");
                break;
            case MediaDesc.VERTICALLY_CONSTRAINED:
                view.setHeight(height + "px");
                break;
            }
            break;

        default:
            view = new Label(path);
            break;
        }

        return view;
    }
}
