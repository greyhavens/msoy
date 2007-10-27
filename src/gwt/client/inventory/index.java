//
// $Id$

package client.inventory;

import com.google.gwt.core.client.GWT;

import com.threerings.msoy.item.data.all.Item;

import client.shell.Args;
import client.shell.Page;
import client.util.MsoyUI;

/**
 * Handles the MetaSOY inventory application.
 */
public class index extends Page
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
    public void onHistoryChanged (Args args)
    {
        updateInterface(args);
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

    protected void updateInterface (Args args)
    {
        if (CInventory.ident == null) {
            // if we have no creds, just display a message saying login
            setPageTitle(CInventory.msgs.inventoryTitle());
            setContent(MsoyUI.createLabel(CInventory.msgs.logon(), "infoLabel"));
            _inventory = null;

        } else {
            if (_inventory == null) {
                setPageTitle(CInventory.msgs.inventoryTitle());
                setContent(_inventory = new InventoryPanel(this));
                setPageTabs(_inventory.getTabs());
            }

            byte type = (byte)args.get(0, Item.AVATAR);
            int pageNo = args.get(1, 0);
            int itemId = args.get(2, 0);
            _inventory.display(type, pageNo, itemId);
        }
    }

    protected InventoryPanel _inventory;
}
