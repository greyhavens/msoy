//
// $Id$

package client.inventory;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;
import com.google.gwt.user.client.ui.Label;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.web.data.WebCreds;

import client.item.ItemEntryPoint;
import client.shell.MsoyEntryPoint;
import client.shell.ShellContext;

/**
 * Handles the MetaSOY inventory application.
 */
public class index extends ItemEntryPoint
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
    protected ShellContext createContext ()
    {
        return _ctx = new InventoryContext();
    }

    // @Override // from MsoyEntryPoint
    protected void initContext ()
    {
        super.initContext();

        // load up our translation dictionaries
        _ctx.imsgs = (InventoryMessages)GWT.create(InventoryMessages.class);
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
        if (_ctx.creds == null) {
            // if we have no creds, just display a message saying login
            setContent(new Label("Log in above to access your inventory."));
            _inventory = null;

        } else {
            if (_inventory == null) {
                setContent(_inventory = new InventoryPanel(_ctx));
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

    protected InventoryContext _ctx;
    protected InventoryPanel _inventory;
}
