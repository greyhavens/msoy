//
// $Id$

package client.util;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.StaticMediaDesc;

import com.threerings.gwt.ui.WidgetUtil;

/**
 * Utility routines for displaying media.
 */
public class MediaUtil
{
    /**
     * Create a widget to display the supplied media.
     *
     * @param size either {@link MediaDesc#HALF_THUMBNAIL_SIZE}, {@link MediaDesc#THUMBNAIL_SIZE}
     * or {@link MediaDesc#PREVIEW_SIZE}.
     */
    public static Widget createMediaView (MediaDesc desc, int size)
    {
        return createMediaView(desc, size, null);
    }

    /**
     * Create a widget to display the supplied media.
     */
    public static Widget createMediaView (MediaDesc desc, int size, ClickListener click)
    {
        return createMediaView(
            desc, MediaDesc.DIMENSIONS[2*size], MediaDesc.DIMENSIONS[2*size+1], click);
    }

    /**
     * Create a widget to display the supplied media. The media will be configured to scale
     * properly to constraint it to the indicated size. The supplied target width and height which
     * must be in the same ratio as the ratio between {@link MediaDesc#THUMBNAIL_WIDTH} and {@link
     * MediaDesc#THUMBNAIL_HEIGHT}.
     */
    public static Widget createMediaView (
        MediaDesc desc, int width, int height, ClickListener click)
    {
        String path = desc.getMediaPath();
        Widget view;

        switch (desc.mimeType) {
        case MediaDesc.VIDEO_YOUTUBE_DEPRECATED:
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
                    view.setHeight("auto");
                    view.setWidth(width + "px");
                }
                break;
            case MediaDesc.HALF_VERTICALLY_CONSTRAINED:
                if (height < MediaDesc.THUMBNAIL_HEIGHT) {
                    view.setHeight(height + "px");
                    view.setWidth("auto");
                }
                break;
            case MediaDesc.HORIZONTALLY_CONSTRAINED:
                view.setHeight("auto");
                view.setWidth(width + "px");
                break;
            case MediaDesc.VERTICALLY_CONSTRAINED:
                view.setHeight(height + "px");
                view.setWidth("auto");
                break;
            }
            break;

        default:
            view = new Image(UNKNOWN_DESC.getMediaPath());
            break;
        }

        // add the click listener if one was provided
        if (click != null && view instanceof Image) {
            ((Image)view).addClickListener(click);
            view.addStyleName("actionLabel");
        }

        return view;
    }

    // TODO: create a proper default image for media we don't know how to display
    protected static final MediaDesc UNKNOWN_DESC = new StaticMediaDesc(
        MediaDesc.IMAGE_PNG, "document", "thumb", MediaDesc.HALF_VERTICALLY_CONSTRAINED);
}
