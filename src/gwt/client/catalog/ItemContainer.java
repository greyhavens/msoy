//
// $Id$

package client.catalog;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Image;
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
public class ItemContainer extends FlexTable
{
    public ItemContainer (final CatalogListing listing, final ItemPanel panel)
    {
        setCellPadding(0);
        setCellSpacing(0);
        setStyleName("itemContainer");

        ClickListener clicker = new ClickListener() {
            public void onClick (Widget sender) {
                new ListingDetailPopup(listing, panel).show();
            }
        };

        Widget preview = MediaUtil.createMediaView(
            listing.item.getThumbnailMedia(), MediaDesc.THUMBNAIL_SIZE);
        if (preview instanceof Image) {
            ((Image)preview).addClickListener(clicker);
            preview.addStyleName("actionLabel");
        }
        setWidget(0, 0, preview);
        getFlexCellFormatter().setStyleName(0, 0, "Preview");

        FlexTable bits = new FlexTable();
        bits.setCellPadding(0);
        bits.setCellSpacing(0);
        Label name = new Label(ItemUtil.getName(listing.item, true));
        name.setStyleName("Name");
        name.addClickListener(clicker);
        bits.setWidget(0, 0, name);

        bits.setWidget(0, 1, new Image("/images/header/symbol_gold.png"));
        bits.getFlexCellFormatter().setWidth(0, 1, "15px");
        bits.getFlexCellFormatter().setHorizontalAlignment(0, 1, HasAlignment.ALIGN_RIGHT);
        bits.setText(0, 2, String.valueOf(listing.goldCost));

        Label creator = new Label(CCatalog.msgs.itemBy(listing.creator.toString()));
        creator.setStyleName("Creator");
        bits.setWidget(1, 0, creator);

        bits.setWidget(1, 1, new Image("/images/header/symbol_flow.png"));
        bits.getFlexCellFormatter().setWidth(1, 1, "15px");
        bits.getFlexCellFormatter().setHorizontalAlignment(1, 1, HasAlignment.ALIGN_RIGHT);
        bits.setText(1, 2, String.valueOf(listing.flowCost));
        setWidget(1, 0, bits);
        getFlexCellFormatter().setHorizontalAlignment(1, 0, HasAlignment.ALIGN_CENTER);
    }
}
