//
// $Id$

package client.inventory;

import com.google.gwt.user.client.ui.TabPanel;

import com.threerings.msoy.web.client.ItemServiceAsync;
import com.threerings.msoy.web.client.WebCreds;

/**
 * Displays a tabbed panel containing a player's inventory.
 */
public class InventoryPanel extends TabPanel
{
    public InventoryPanel (WebCreds creds, ItemServiceAsync itemsvc)
    {
        setStyleName("inventory");
        // create item panels for our known item types (alas we can't use
        // ItemEnum here)
        add(new ItemPanel(creds, itemsvc, "PHOTO"), "Photos");
        add(new ItemPanel(creds, itemsvc, "DOCUMENT"), "Documents");
        selectTab(0);
    }
}
