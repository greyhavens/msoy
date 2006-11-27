//
// $Id$

package client.catalog;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;

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
        RootPanel.get("content").clear();

        // if we have no creds, just display a message saying login
        if (_ctx.creds == null) {
            RootPanel.get("content").add(
                new Label("Log in above to access the catalog."));
            return;
        }

        if (token.equals("upload")) {
            // TODO

        } else { // "inventory" or hacked URL
            if (_catalog == null) {
                _catalog = new CatalogPanel(_ctx);
            }
            RootPanel.get("content").add(_catalog);
        }
    }

    // @Override // from MsoyEntryPoint
    protected String getPageId ()
    {
        return "catalog";
    }

    // @Override from MsoyEntryPoint
    protected void onPageLoad ()
    {
        History.addHistoryListener(this);
        String initToken = History.getToken();
        if (initToken.length() > 0) {
            onHistoryChanged(initToken);
        } else {
            onHistoryChanged("catalog");
        }
    }

    // @Override from MsoyEntryPoint
    protected void didLogon (WebCreds creds)
    {
        super.didLogon(creds);
        onHistoryChanged("catalog");
    }

    // @Override from MsoyEntryPoint
    protected void didLogoff ()
    {
        super.didLogoff();
        _catalog = null;
        onHistoryChanged("catalog");
    }

    protected CatalogPanel _catalog;
}
