//
// $Id$

package client.catalog;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.item.web.CatalogListing;
import com.threerings.msoy.item.web.Item;

import client.item.ItemUtil;

/**
 * Displays a catalog listing.
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

        add(ItemUtil.createMediaView(item.getThumbnailMedia(), true));

        Label descrip = new Label(truncateDescription(item.getDescription()));
        descrip.setStyleName("itemDescrip");
        add(descrip);

        Label creator = new Label("by " + listing.creator);
        creator.setStyleName("itemCreator");
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
     * Convenience method to truncate the specified description to fit.
     */
    protected String truncateDescription (String text)
    {
        return (text.length() <= 32) ? text : (text.substring(0, 29) + "...");
    }
}
