//
// $Id$

package client.catalog;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.web.CatalogListing;
import com.threerings.msoy.item.web.Item;

import client.item.ItemUtil;

/**
 * Displays a catalog listing.
 */
public class ItemContainer extends VerticalPanel
{
    public ItemContainer (final CatalogListing listing, final ItemPanel panel)
    {
        add(ItemUtil.createMediaView(listing.item.getThumbnailMedia(), true));

        Label descrip = new Label(truncateDescription(listing.item.getDescription()));
        descrip.setStyleName("itemDescrip");
        descrip.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                new ListingDetailPopup(panel._ctx, listing, panel).show();
            }
        });
        add(descrip);

        Label creator = new Label("by " + listing.creator);
        creator.setStyleName("itemCreator");
        add(creator);
    }

    /**
     * Convenience method to truncate the specified description to fit.
     */
    protected String truncateDescription (String text)
    {
        return (text.length() <= 32) ? text : (text.substring(0, 29) + "...");
    }
}
