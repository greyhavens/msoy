//
// $Id$

package client.stuff;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.SubItem;

import client.shell.Args;
import client.shell.CShell;
import client.shell.Page;
import client.util.Link;

/**
 * Extends {@link CShell} and provides stuff-specific services.
 */
public class CStuff extends CShell
{
    /** Messages used by the stuff interfaces. */
    public static StuffMessages msgs;

    public static void viewParent (Item item)
    {
        if (item instanceof SubItem) {
            SubItem sitem = (SubItem)item;
            viewItem(sitem.getSuiteMasterType(), sitem.suiteId);
        } else {
            Link.go(Page.STUFF, ""+item.getType());
        }
    }

    public static void viewItem (byte type, int itemId)
    {
        Link.go(Page.STUFF, Args.compose(""+type, "-1", ""+itemId));
    }

    public static void editItem (byte type, int itemId)
    {
        Link.go(Page.STUFF, Args.compose("e", ""+type, ""+itemId));
    }

    public static void remixItem (byte type, int itemId)
    {
        Link.go(Page.STUFF, Args.compose("r", ""+type, ""+itemId));
    }

    public static void remixCatalogItem (
        byte type, int itemId, int catalogId, int flowCost, int goldCost)
    {
        Link.go(Page.STUFF, Args.compose(
            new String[] { "r", ""+type, ""+itemId, ""+catalogId, ""+flowCost, ""+goldCost }));
    }

    public static void createItem (byte type, byte ptype, int pitemId)
    {
        Link.go(Page.STUFF, Args.compose(new String[] { "c", ""+type, ""+ptype, ""+pitemId }));
    }
}
