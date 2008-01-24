//
// $Id$

package client.inventory;

import client.shell.Application;
import client.shell.Args;
import client.shell.CShell;
import client.shell.Page;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.SubItem;

/**
 * Extends {@link CShell} and provides inventory-specific services.
 */
public class CInventory extends CShell
{
    /** Messages used by the inventory interfaces. */
    public static InventoryMessages msgs;

    public static void viewParent (Item item)
    {
        if (item instanceof SubItem) {
            SubItem sitem = (SubItem)item;
            viewItem(sitem.getSuiteMasterType(), sitem.suiteId);
        } else {
            Application.go(Page.INVENTORY, ""+item.getType());
        }
    }

    public static void viewItem (byte type, int itemId)
    {
        Application.go(Page.INVENTORY, Args.compose(""+type, "-1", ""+itemId));
    }

    public static void editItem (byte type, int itemId)
    {
        Application.go(Page.INVENTORY, Args.compose("e", ""+type, ""+itemId));
    }

    public static void remixItem (byte type, int itemId)
    {
        Application.go(Page.INVENTORY, Args.compose("r", ""+type, ""+itemId));
    }

    public static void createItem (byte type, byte ptype, int pitemId)
    {
        Application.go(Page.INVENTORY, Args.compose(new String[] {
                    "c", ""+type, ""+ptype, ""+pitemId }));
    }
}
