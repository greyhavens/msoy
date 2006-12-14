//
// $Id$

package client.inventory;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;
import com.google.gwt.user.client.ui.Label;

import com.threerings.msoy.web.data.WebCreds;

import client.shell.MsoyEntryPoint;

/**
 * Handles the MetaSOY inventory application.
 */
public class index extends MsoyEntryPoint
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
        updateInterface(History.getToken());
    }

    // @Override from MsoyEntryPoint
    protected void didLogon (WebCreds creds)
    {
        super.didLogon(creds);
        updateInterface(null);
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
                _inventory = new InventoryPanel(_ctx);
                setContent(_inventory);
            }
            // TODO: set page based on history token
        }
    }

    protected InventoryPanel _inventory;
}
