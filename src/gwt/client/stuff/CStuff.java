//
// $Id$

package client.stuff;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.SubItem;

import client.shell.CShell;
import client.shell.Pages;
import client.util.Link;
import client.util.NaviUtil;

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
            NaviUtil.viewItem(sitem.getSuiteMasterType(), sitem.suiteId);
        } else {
            Link.go(Pages.STUFF, ""+item.getType());
        }
    }
}
