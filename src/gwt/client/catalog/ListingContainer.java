//
// $Id$

package client.catalog;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.gwt.CatalogListing;

import client.util.ItemUtil;
import client.util.MediaUtil;
import client.util.MsoyUI;

/**
 * Displays a catalog listing.
 */
public class ListingContainer extends FlexTable
{
    public ListingContainer (final CatalogListing listing, final CatalogPanel panel)
    {
        setCellPadding(0);
        setCellSpacing(0);
        setStyleName("listingContainer");

        ClickListener clicker = new ClickListener() {
            public void onClick (Widget sender) {
                panel.showListing(listing);
            }
        };

        Widget preview = MediaUtil.createMediaView(
            listing.item.getThumbnailMedia(), MediaDesc.HALF_THUMBNAIL_SIZE);
        if (preview instanceof Image) {
            ((Image)preview).addClickListener(clicker);
            preview.addStyleName("actionLabel");
        }
        setWidget(0, 0, preview);
        getFlexCellFormatter().setStyleName(0, 0, "Preview");
        getFlexCellFormatter().setRowSpan(0, 0, 4);

        setWidget(0, 1, MsoyUI.createActionLabel(
                      ItemUtil.getName(listing.item, true), "Name", clicker));
        getFlexCellFormatter().setColSpan(0, 1, 3);

        setText(1, 0, CCatalog.msgs.itemBy(listing.creator.toString()));
        getFlexCellFormatter().setStyleName(1, 0, "Creator");
        getFlexCellFormatter().setColSpan(1, 0, 3);

        setText(2, 0, "(rating)");
        getFlexCellFormatter().setStyleName(2, 0, "Cost");

        setWidget(2, 1, new Image("/images/header/symbol_flow.png"));
        getFlexCellFormatter().setWidth(2, 1, "15px");
        getFlexCellFormatter().setHorizontalAlignment(2, 1, HasAlignment.ALIGN_RIGHT);
        setText(2, 2, String.valueOf(listing.flowCost));
        getFlexCellFormatter().setStyleName(2, 2, "Cost");

//         bits.setWidget(0, 1, new Image("/images/header/symbol_gold.png"));
//         bits.getFlexCellFormatter().setWidth(0, 1, "15px");
//         bits.getFlexCellFormatter().setHorizontalAlignment(0, 1, HasAlignment.ALIGN_RIGHT);
//         bits.setText(0, 2, String.valueOf(listing.goldCost));
    }
}
