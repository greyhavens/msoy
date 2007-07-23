//
// $Id$

package client.inventory;

import com.google.gwt.core.client.GWT;
import com.threerings.msoy.item.data.all.Item;
import client.editem.EditemEntryPoint;
import client.shell.Page;
import client.util.MsoyUI;

/**
 * Handles the MetaSOY inventory application.
 */
public class index extends EditemEntryPoint
{
    /** Required to map this entry point to a page. */
    public static Creator getCreator ()
    {
        return new Creator() {
            public Page createPage () {
                return new index();
            }
        };
    }

    // @Override from Page
    public void onHistoryChanged (String token)
    {
        updateInterface(token);
    }

    // @Override // from Page
    protected String getPageId ()
    {
        return "inventory";
    }

    // @Override // from Page
    protected void initContext ()
    {
        super.initContext();

        // load up our translation dictionaries
        CInventory.msgs = (InventoryMessages)GWT.create(InventoryMessages.class);
    }

    protected void updateInterface (String args)
    {
        setPageTitle(CInventory.msgs.inventoryTitle());
        if (CInventory.ident == null) {
            // if we have no creds, just display a message saying login
            setContent(MsoyUI.createLabel(CInventory.msgs.logon(), "infoLabel"));
            _inventory = null;

        } else {
            if (_inventory == null) {
                setContent(_inventory = new InventoryPanel());
                setPageTabs(_inventory.getTabs());
            }

            int[] avals = Page.splitArgs(args);
            byte type = (avals[0] == 0) ? Item.AVATAR : (byte)avals[0];
            int pageNo = (avals.length > 1) ? avals[1] : 0;
            int itemId = (avals.length > 2) ? avals[2] : 0;
            _inventory.display(type, pageNo, itemId);
        }
    }

    protected InventoryPanel _inventory;
}
