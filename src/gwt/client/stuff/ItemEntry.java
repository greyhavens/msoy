//
// $Id$

package client.stuff;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.web.gwt.Pages;

import client.item.ItemActivator;
import client.item.ItemBox;
import client.item.ItemUtil;

/**
 * Displays a thumbnail version of an item.
 */
public class ItemEntry extends ItemBox
{
    public ItemEntry (Item item, boolean activator)
    {
        super(item.getThumbnailMedia(), ItemUtil.getName(item, true),
            item.getPrimaryMedia().isRemixable(),
            item.isAttrSet(Item.ATTR_THEME_STAMPED) ? Pages.STUFF : null,
            "d", item.getType(), item.itemId);

        if (item.itemId > 0) { // if this item is an original, style it slightly differently
            getFlexCellFormatter().addStyleName(1, 0, "Original");
        }
        if (!item.isAttrSet(Item.ATTR_THEME_STAMPED)) {
            this.addStyleName("Shaded");

        } else if (activator) {
            // only allow activation if the item is stamped
            addWidget(new ItemActivator(item, false), getColumns(), "Activator");
        }
    }
}
