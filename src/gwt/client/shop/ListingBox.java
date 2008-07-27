//
// $Id$

package client.shop;

import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Image;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.item.gwt.ListingCard;

import client.shell.Args;
import client.shell.Page;
import client.util.ItemBox;
import client.util.MsoyUI;
import client.util.Stars;

/**
 * Displays a catalog listing.
 */
public class ListingBox extends ItemBox
{
    public ListingBox (ListingCard listing)
    {
        super(listing.getThumbnailMedia(), listing.name, Page.SHOP,
              Args.compose("l", "" + listing.itemType, "" + listing.catalogId), listing.remixable);

        String cname = CShop.msgs.itemBy(listing.creator.toString());
        addWidget(MsoyUI.createLabel(cname, "Creator"), getColumns(), null);

        int row = getRowCount();
        setWidget(row, 0, new Stars(listing.rating, true, true, null), 1, "Rating");

        SmartTable cost = new SmartTable(0, 0);
        setWidget(row, 1, cost);

        cost.setWidget(0, 0, new Image("/images/ui/coins.png"));
        cost.getFlexCellFormatter().setWidth(0, 0, "15px");
        cost.getFlexCellFormatter().setHorizontalAlignment(0, 1, HasAlignment.ALIGN_RIGHT);
        cost.setText(0, 1, String.valueOf(listing.flowCost), 1, "Cost");

        if (listing.goldCost > 0) {
            cost.setWidget(1, 0, new Image("/images/ui/gold.png"));
            cost.getFlexCellFormatter().setWidth(1, 0, "15px");
            cost.getFlexCellFormatter().setHorizontalAlignment(1, 1, HasAlignment.ALIGN_RIGHT);
            cost.setText(1, 1, String.valueOf(listing.goldCost), 1, "Cost");
        }
    }

    @Override // from ItemBox
    protected int getColumns ()
    {
        return 2;
    }
}
