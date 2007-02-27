//
// $Id$

package client.catalog;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;

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

        FlexTable bits = new FlexTable();
        FlexCellFormatter formatter = bits.getFlexCellFormatter();
        
        Label descrip = new Label(ItemUtil.getName(listing.item, true));
        descrip.setStyleName("itemDescrip");
        descrip.addClickListener(clicker);
        formatter.setWidth(0, 0, "100%");
        bits.setWidget(0, 0, descrip);

        Label creator = new Label(CCatalog.msgs.itemBy(listing.creator.toString()));
        creator.setStyleName("itemCreator");
        formatter.setWidth(1, 0, "100%");
        bits.setWidget(1, 0, creator);

        formatter.setWidth(0, 1, "25px"); // gap!
        formatter.setStyleName(0, 1, "Icon");
        bits.setWidget(0, 1, new Image("/images/header/symbol_gold.png"));
        bits.setText(0, 2, String.valueOf(listing.goldCost));

        formatter.setWidth(1, 1, "25px"); // gap!
        formatter.setStyleName(1, 1, "Icon");
        bits.setWidget(1, 1, new Image("/images/header/symbol_flow.png"));
        bits.setText(1, 2, String.valueOf(listing.flowCost));
        
        add(bits);
    }
}
