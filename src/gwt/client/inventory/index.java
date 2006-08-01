//
// $Id$

package client.inventory;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;

import com.threerings.msoy.item.data.Document;
import com.threerings.msoy.web.client.WebCreds;

import client.MsoyEntryPoint;

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

    // @Override from MsoyEntryPoint
    public void onPageLoad ()
    {
//         // set up our navigation
//         HorizontalPanel navi = new HorizontalPanel();
//         navi.setSpacing(5);
//         navi.add(new Hyperlink("Inventory", "inventory"));
//         navi.add(new Hyperlink("Upload", "upload"));
//         RootPanel.get("navi").add(navi);

        History.addHistoryListener(this);
        String initToken = History.getToken();
        if (initToken.length() > 0) {
            onHistoryChanged(initToken);
        } else {
            // default to the user's inventory
            onHistoryChanged("inventory");
        }
    }

    // from interface HistoryListener
    public void onHistoryChanged (String token)
    {
        RootPanel.get("content").clear();

        // if we have no creds, just display a message saying login
        if (_ctx.creds == null) {
            RootPanel.get("content").add(
                new Label("Log in above to access your inventory."));
            return;
        }

        if (token.equals("upload")) {
            // TODO

        } else { // "inventory" or hacked URL
            if (_inventory == null) {
                _inventory = new InventoryPanel(_ctx);
            }
            RootPanel.get("content").add(_inventory);
        }
    }

    // @Override from MsoyEntryPoint
    protected void didLogon (WebCreds creds)
    {
        super.didLogon(creds);
        onHistoryChanged("inventory");
    }

    // @Override from MsoyEntryPoint
    protected void didLogoff ()
    {
        super.didLogoff();
        _inventory = null;
        onHistoryChanged("inventory");
    }

    protected InventoryPanel _inventory;
}
