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

/**
 * Contains utility methods for item related user interface business.
 */
public class ItemUtil
{
    /**
     * Create a widget to display the supplied media.
     *
     * @param thumbnail if true the media will be scaled to the thumbnail size, if false it will be
     * scaled to the preview size.
     */
    public static Widget createMediaView (MediaDesc desc, boolean thumbnail)
    {
        int width = thumbnail ? Item.THUMBNAIL_WIDTH : Item.PREVIEW_WIDTH;
        int height = thumbnail ? Item.THUMBNAIL_HEIGHT : Item.PREVIEW_HEIGHT;
        return createMediaView(desc, width, height);
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

        view.setStyleName("itemMediaView");
        return view;
    }
}
