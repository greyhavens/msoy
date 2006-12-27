//
// $Id$

package client.catalog;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.web.CatalogListing;
import com.threerings.msoy.item.web.MediaDesc;

import client.item.ItemUtil;
import client.util.MediaUtil;

/**
 * Displays a catalog listing.
 */
public class ItemContainer extends VerticalPanel
{
    public ItemContainer (final CatalogContext ctx, final CatalogListing listing,
                          final ItemPanel panel)
    {
        add(MediaUtil.createMediaView(listing.item.getThumbnailMedia(), MediaDesc.THUMBNAIL_SIZE));

        Label descrip = new Label(ItemUtil.getName(panel._ctx, listing.item, true));
        descrip.setStyleName("itemDescrip");
        descrip.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                new ListingDetailPopup(ctx, listing, panel).show();
            }
        });
        add(descrip);

        Label creator = new Label(ctx.msgs.itemBy(listing.creator.toString()));
        creator.setStyleName("itemCreator");
        add(creator);
    }
}
