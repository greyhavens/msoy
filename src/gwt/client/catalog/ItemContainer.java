//
// $Id$

package client.catalog;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Image;
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
    public ItemContainer (final CatalogListing listing, final ItemPanel panel)
    {
        setHorizontalAlignment(ALIGN_CENTER);
        setVerticalAlignment(ALIGN_MIDDLE);

        ClickListener clicker = new ClickListener() {
            public void onClick (Widget sender) {
                new ListingDetailPopup(listing, panel).show();
            }
        };

        Widget mview = MediaUtil.createMediaView(
            listing.item.getThumbnailMedia(), MediaDesc.THUMBNAIL_SIZE);
        if (mview instanceof Image) {
            ((Image)mview).addClickListener(clicker);
            mview.addStyleName("actionLabel");
        }
        add(mview);

        Label descrip = new Label(ItemUtil.getName(listing.item, true));
        descrip.setStyleName("itemDescrip");
        descrip.addClickListener(clicker);
        add(descrip);

        Label creator = new Label(CCatalog.msgs.itemBy(listing.creator.toString()));
        creator.setStyleName("itemCreator");
        add(creator);
    }
}
