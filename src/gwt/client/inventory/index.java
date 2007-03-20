//
// $Id$

package client.inventory;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Label;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.web.data.WebCreds;

import client.editem.EditemEntryPoint;
import client.item.ItemEntryPoint;
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
        if (CInventory.creds == null) {
            // if we have no creds, just display a message saying login
            setContent(MsoyUI.createLabel(CInventory.msgs.logon(), "infoLabel"));
            _inventory = null;

        } else {
            if (_inventory == null) {
                setContent(_inventory = new InventoryPanel());
            }
            byte type = Item.AVATAR;
            try {
                if (args != null) {
                    type = Byte.parseByte(args);
                }
            } catch (Exception e) {
                // whatever, just show the default
            }
            _inventory.selectType(type);
        }
    }

    protected InventoryPanel _inventory;
}
