//
// $Id$

package client.shop;

import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Image;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.msoy.item.data.gwt.CatalogListing;

import client.item.ItemRating;
import client.shell.Args;
import client.shell.Page;
import client.util.ItemBox;
import client.util.ItemUtil;
import client.util.Stars;

/**
 * Displays a catalog listing.
 */
public class ListingContainer extends ItemBox
{
    public ListingContainer (CatalogListing listing)
    {
        super(listing.item.getThumbnailMedia(), ItemUtil.getName(listing.item, true), Page.SHOP,
              Args.compose(new String[] {
                  "" + listing.item.getType(), CatalogPanel.ONE_LISTING, "" + listing.catalogId
              }));

        addText(CShop.msgs.itemBy(listing.creator.toString()), 2, "Creator");

        int row = getRowCount();
        setWidget(row, 0, new ItemRating(listing.item, Stars.NO_RATING,
                                         Stars.MODE_READ, true, false), 1, "Rating");

        SmartTable cost = new SmartTable(0, 0);
        setWidget(row, 1, cost);

        cost.setWidget(0, 0, new Image("/images/header/symbol_flow.png"));
        cost.getFlexCellFormatter().setWidth(0, 0, "15px");
        cost.getFlexCellFormatter().setHorizontalAlignment(0, 1, HasAlignment.ALIGN_RIGHT);
        cost.setText(0, 1, String.valueOf(listing.flowCost), 1, "Cost");

        if (listing.goldCost > 0) {
            cost.setWidget(1, 0, new Image("/images/header/symbol_gold.png"));
            cost.getFlexCellFormatter().setWidth(1, 0, "15px");
            cost.getFlexCellFormatter().setHorizontalAlignment(1, 1, HasAlignment.ALIGN_RIGHT);
            cost.setText(1, 1, String.valueOf(listing.goldCost), 1, "Cost");
        }
    }

    // @Override // from ItemBox
    protected int getColumns ()
    {
        return 2;
    }
}
