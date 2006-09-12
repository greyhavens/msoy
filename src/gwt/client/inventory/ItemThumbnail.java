//
// $Id$

package client.inventory;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.Item;
import com.threerings.msoy.item.data.MediaItem;
import com.threerings.msoy.item.data.Photo;

import client.MsoyEntryPoint;

import client.util.FlashWidget;

/**
 * Displays a thumbnail version of an item.
 *
 * <p> Styles:
 * <ul>
 * <li> item_thumb_image - the style of item thumbnail image
 * <li> item_thumb_text - the style of item thumbnail text
 * </ul>
 */
public class ItemThumbnail extends VerticalPanel
{
    public ItemThumbnail (Item item)
    {
        Widget thumb = createThumbnail(item);
        thumb.setStyleName("item_thumb_image");
        thumb.setPixelSize(THUMB_WIDTH, THUMB_HEIGHT);
        add(thumb);
        Label label = new Label(item.getInventoryDescrip());
        label.setStyleName("item_thumb_text");
        add(label);
    }

    /**
     * Helper method to create the thumbnail widget.
     */
    protected Widget createThumbnail (Item item)
    {
        if (item instanceof MediaItem) {
            MediaItem mitem = (MediaItem) item;
            if (mitem.mimeType == MediaItem.APPLICATION_SHOCKWAVE_FLASH) {
                String ident = MediaItem.hashToString(mitem.mediaHash);
                FlashWidget fw = new FlashWidget(ident);
                fw.setMovie(MsoyEntryPoint.toMediaPath(mitem.getMediaPath()));
                return fw;
            }
        }

        return new Image(
            MsoyEntryPoint.toMediaPath(item.getThumbnailPath()));
    }

    /** So arbitrary. TODO. */
    public static final int THUMB_WIDTH = 250;
    public static final int THUMB_HEIGHT = 200;
}
