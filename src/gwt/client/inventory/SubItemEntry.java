//
// $Id$

package client.inventory;

import com.threerings.msoy.item.data.all.Item;

/**
 * Displays information on a sub-item.
 */
public class SubItemEntry extends ItemEntry
{
    public SubItemEntry (ItemPanel panel, Item item)
    {
        super(panel, item, null);
    }

    // @Override // from ItemEntry
    public void setItem (Item item)
    {
        super.setItem(item);

        getFlexCellFormatter().setRowSpan(0, 0, 3);
        setText(1, 0, "Extra bits");
    }
}
