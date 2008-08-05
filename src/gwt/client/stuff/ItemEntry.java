//
// $Id$

package client.stuff;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.SubItem;

import client.item.ItemActivator;
import client.item.ItemBox;
import client.item.ItemUtil;
import client.shell.Args;
import client.shell.Pages;

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
