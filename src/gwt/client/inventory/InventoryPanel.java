//
// $Id$

package client.inventory;

import com.google.gwt.user.client.ui.TabPanel;

import com.threerings.msoy.web.client.WebContext;

import com.threerings.msoy.item.web.Item;

/**
 * Displays a tabbed panel containing a player's inventory.
 */
public class InventoryPanel extends TabPanel
{
    public InventoryPanel (WebContext ctx)
    {
        setStyleName("inventory");
        // create item panels for our known item types (alas we can't use
        // ItemEnum here)
        add(new ItemPanel(ctx, Item.PHOTO), "Photos");
        add(new ItemPanel(ctx, Item.DOCUMENT), "Documents");
        add(new ItemPanel(ctx, Item.FURNITURE), "Furniture");
        add(new ItemPanel(ctx, Item.GAME), "Games");
        add(new ItemPanel(ctx, Item.AVATAR), "Avatars");
        add(new ItemPanel(ctx, Item.PET), "Pets");
        selectTab(0);
    }
}
