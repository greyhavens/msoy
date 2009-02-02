//
// $Id$

package client.item;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Image;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.gwt.ListingCard;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.ui.MsoyUI;
import client.ui.Stars;

/**
 * Displays a catalog listing.
 */
public class ListingBox extends ItemBox
{
    public static ListingBox newBox (ListingCard card)
    {
        // games link to the game shop instead of to the game item
        String action = (card.itemType == Item.GAME) ? "s" : "l";
        return new ListingBox(card, Args.compose(action, card.itemType, card.catalogId));
    }

    public static ListingBox newSubBox (ListingCard card)
    {
        // sub-boxes always link to the item itself
        return new ListingBox(card, Args.compose("l", card.itemType, card.catalogId));
    }

    protected ListingBox (ListingCard card, String args)
    {
        super(card.thumbMedia, card.name, Pages.SHOP, args, card.remixable);

        String cname = _imsgs.itemBy(card.creator.toString());
        addLabel(MsoyUI.createLabel(cname, "Creator"));

        int row = getRowCount();
        setWidget(row, 0, new Stars(card.rating, true, true, null), 1, "Rating");

        SmartTable cost = new SmartTable(0, 0);
        setWidget(row, 1, cost);

        cost.setWidget(0, 0, new Image(card.currency.getSmallIcon()));
        cost.getFlexCellFormatter().setWidth(0, 0, "15px");
        cost.getFlexCellFormatter().setHorizontalAlignment(0, 1, HasAlignment.ALIGN_RIGHT);
        cost.setText(0, 1, card.currency.format(card.cost), 1, "Cost");
    }

    @Override // from ItemBox
    protected int getColumns ()
    {
        return 2;
    }

    protected static final ItemMessages _imsgs = GWT.create(ItemMessages.class);
}
