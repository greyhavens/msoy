//
// $Id$

package client.catalog;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.web.CatalogListing;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.MediaDesc;

import client.MsoyEntryPoint;
import client.util.WidgetUtil;

/**
 * Displays a catalog listing.
 *
 * TODO: Refactor relative to inventory.ItemContainer
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

    public ItemContainer (CatalogListing listing, final ItemPanel panel)
    {
        final Item item = listing.item;

        Widget disp = createContainer(item);
        disp.setStyleName("item_thumb_image");
        disp.setHeight(THUMB_HEIGHT + "px");
        add(disp);

        Label descrip = new Label(truncateDescription(item.getDescription()));
        descrip.setStyleName("item_thumb_text");
        add(descrip);

        Label creator = new Label("by " + listing.creator.memberName);
        creator.setStyleName("item_creator_text");
        add(creator);

        Button button = new Button("Buy!");
        button.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                panel.purchaseItem(item.getIdent());
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
        switch (MediaDesc.suffixToMimeType(thumbPath)) {
        case MediaDesc.APPLICATION_SHOCKWAVE_FLASH:
            String ident = String.valueOf(item.itemId);
            return WidgetUtil.createFlashContainer(
                // TODO: allow passing -1 for width
                ident, thumbPath, THUMB_HEIGHT, THUMB_HEIGHT, null);

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
