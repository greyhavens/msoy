//
// $Id$

package client.util;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.StaticMediaDesc;
import com.threerings.msoy.web.gwt.SharedMediaUtil;

import com.threerings.gwt.ui.WidgetUtil;

/**
 * Extends the server-side shared media utilities with client specific routines for displaying
 * media.
 */
public class MediaUtil extends SharedMediaUtil
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
    public static Widget createMediaView (MediaDesc desc, int size, ClickHandler click)
    {
        return createMediaView(desc, MediaDesc.getWidth(size), MediaDesc.getHeight(size), click);
    }

    /**
     * Create a widget to display the supplied media. The media will be configured to scale
     * properly to constraint it to the indicated size. The supplied target width and height which
     * must be in the same ratio as the ratio between {@link MediaDesc#THUMBNAIL_WIDTH} and {@link
     * MediaDesc#THUMBNAIL_HEIGHT}.
     */
    public static Widget createMediaView (
        MediaDesc desc, int width, int height, ClickHandler click)
    {
        String path = desc.getMediaPath();
        Widget view;

        if (desc.isSWF()) {
            view = WidgetUtil.createFlashContainer("", path, width, height, null);

        } else if (desc.isVideo()) {
            view = FlashClients.createVideoPlayer(width, height, path);

        } else if (desc.isAudio()) {
            view = FlashClients.createAudioPlayer(width, height, path);

        } else if (desc.isImage()) {
            view = new Image(path);
            Dimensions dims = resolveImageSize(desc, width, height);
            if (dims != null) {
                view.setWidth(dims.width);
                view.setHeight(dims.height);
            }

        } else {
            view = new Image(UNKNOWN_DESC.getMediaPath());
        }

        // add the click listener if one was provided
        if (click != null && view instanceof Image) {
            ((Image)view).addClickHandler(click);
            view.addStyleName("actionLabel");
        }

        return view;
    }

    // TODO: create a proper default image for media we don't know how to display
    protected static final MediaDesc UNKNOWN_DESC = new StaticMediaDesc(
        MediaDesc.IMAGE_PNG, "document", "thumb", MediaDesc.HALF_VERTICALLY_CONSTRAINED);
}
