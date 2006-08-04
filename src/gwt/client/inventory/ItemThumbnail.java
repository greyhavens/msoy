//
// $Id$

package client.inventory;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;

import com.threerings.msoy.item.data.Item;
import com.threerings.msoy.item.data.Photo;

import client.MsoyEntryPoint;

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
        Image image = new Image(
            MsoyEntryPoint.toMediaPath(item.getThumbnailPath()));
        image.setStyleName("item_thumb_image");
        add(image);
        Label label = new Label(item.getInventoryDescrip());
        label.setStyleName("item_thumb_text");
        add(label);
    }
}
