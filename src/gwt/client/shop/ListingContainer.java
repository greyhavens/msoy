//
// $Id$

package client.shop;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.gwt.CatalogListing;

import client.item.ItemRating;
import client.shell.Application;
import client.shell.Args;
import client.shell.Page;
import client.util.ItemUtil;
import client.util.MediaUtil;
import client.util.MsoyUI;
import client.util.Stars;

/**
 * Displays a catalog listing.
 */
public class ListingContainer extends FlexTable
{
    public ListingContainer (final CatalogListing listing)
    {
        setCellPadding(0);
        setCellSpacing(0);
        setStyleName("listingContainer");

        ClickListener clicker = new ClickListener() {
            public void onClick (Widget sender) {
                Application.go(Page.SHOP, Args.compose(new String[] {
                    "" + listing.item.getType(), CatalogPanel.ONE_LISTING, "" + listing.catalogId
                }));
            }
        };
        setWidget(0, 0, MediaUtil.createMediaView(
                      listing.item.getThumbnailMedia(), MediaDesc.THUMBNAIL_SIZE, clicker));
        getFlexCellFormatter().setStyleName(0, 0, "Preview");
        getFlexCellFormatter().setRowSpan(0, 0, 4);

        setWidget(0, 1, MsoyUI.createActionLabel(
                      ItemUtil.getName(listing.item, true), "Name", clicker));
        getFlexCellFormatter().setColSpan(0, 1, 3);

        setText(1, 0, CCatalog.msgs.itemBy(listing.creator.toString()));
        getFlexCellFormatter().setStyleName(1, 0, "Creator");
        getFlexCellFormatter().setColSpan(1, 0, 3);

        setWidget(2, 0, new ItemRating(listing.item, Stars.NO_RATING, Stars.MODE_READ, true, false));
        getFlexCellFormatter().setStyleName(2, 0, "Rating");

        setWidget(2, 1, new Image("/images/header/symbol_flow.png"));
        getFlexCellFormatter().setWidth(2, 1, "15px");
        getFlexCellFormatter().setHorizontalAlignment(2, 1, HasAlignment.ALIGN_RIGHT);
        setText(2, 2, String.valueOf(listing.flowCost));
        getFlexCellFormatter().setStyleName(2, 2, "Cost");

        if (listing.goldCost > 0) {
            setWidget(3, 1, new Image("/images/header/symbol_gold.png"));
            getFlexCellFormatter().setWidth(3, 1, "15px");
            getFlexCellFormatter().setHorizontalAlignment(3, 1, HasAlignment.ALIGN_RIGHT);
            setText(3, 2, String.valueOf(listing.goldCost));
            getFlexCellFormatter().setStyleName(3, 2, "Cost");
        }
    }
}
