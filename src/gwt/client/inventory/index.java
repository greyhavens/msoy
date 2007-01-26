//
// $Id$

package client.inventory;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;
import com.google.gwt.user.client.ui.Label;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.web.data.WebCreds;

import client.editem.EditemEntryPoint;
import client.item.ItemEntryPoint;
import client.shell.MsoyEntryPoint;

/**
 * Handles the MetaSOY inventory application.
 */
public class index extends EditemEntryPoint
    implements HistoryListener
{
    /** Required to map this entry point to a page. */
    public static Creator getCreator ()
    {
        return new Creator() {
            public MsoyEntryPoint createEntryPoint () {
                return new index();
            }
        };
    }

    // from interface HistoryListener
    public void onHistoryChanged (String token)
    {
        updateInterface(token);
    }

    // @Override // from MsoyEntryPoint
    protected String getPageId ()
    {
        return "inventory";
    }

    // @Override from MsoyEntryPoint
    protected void onPageLoad ()
    {
        History.addHistoryListener(this);
    }

    // @Override // from MsoyEntryPoint
    protected void initContext ()
    {
        super.initContext();

        // load up our translation dictionaries
        CInventory.msgs = (InventoryMessages)GWT.create(InventoryMessages.class);
    }

    // @Override from MsoyEntryPoint
    protected void didLogon (WebCreds creds)
    {
        super.didLogon(creds);
        updateInterface(History.getToken());
    }

    // @Override from MsoyEntryPoint
    protected void didLogoff ()
    {
        super.didLogoff();
        updateInterface(null);
    }

    protected void updateInterface (String historyToken)
    {
        if (CInventory.creds == null) {
            // if we have no creds, just display a message saying login
            setContent(new Label("Log in above to access your inventory."));
            _inventory = null;

        } else {
            if (_inventory == null) {
                setContent(_inventory = new InventoryPanel());
            }
            byte type = Item.AVATAR;
            try {
                if (historyToken != null) {
                    type = Byte.parseByte(historyToken);
                }
            } catch (Exception e) {
                // whatever, just show the default
            }
            _inventory.selectType(type);
        }
    }

    protected InventoryPanel _inventory;
}
