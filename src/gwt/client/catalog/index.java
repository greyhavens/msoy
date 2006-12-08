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
        // TODO: anything?
    }

    // @Override // from MsoyEntryPoint
    protected String getPageId ()
    {
        return "catalog";
    }

    // @Override from MsoyEntryPoint
    protected void onPageLoad ()
    {
        _catalog = new CatalogPanel(_ctx);
        setContent(_catalog);

        History.addHistoryListener(this);
        String initToken = History.getToken();
        if (initToken.length() > 0) {
            onHistoryChanged(initToken);
        } else {
            onHistoryChanged("catalog");
        }
    }

    protected CatalogPanel _catalog;
}
