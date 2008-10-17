//
// $Id$

package client.stuff;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.SubItem;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.item.ItemActivator;
import client.item.ItemBox;
import client.item.ItemUtil;

/**
 * Displays a thumbnail version of an item.
 */
public class ItemEntry extends ItemBox
{
    public ItemEntry (Item item)
    {
        super(item.getThumbnailMedia(), ItemUtil.getName(item, true), Pages.STUFF,
              Args.compose("d", ""+item.getType(), ""+item.itemId),
              item.getPrimaryMedia().isRemixable());

        if (item.itemId > 0) { // if this item is an original, style it slightly differently
            getFlexCellFormatter().addStyleName(1, 0, "Original");
        }

        if (!(item instanceof SubItem)) {
            addWidget(new ItemActivator(item, false), getColumns(), "Activator");
        }
    }
}
