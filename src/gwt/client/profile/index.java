//
// $Id$

package client.profile;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;
import com.google.gwt.user.client.ui.RootPanel;

import client.MsoyEntryPoint;

/**
 * Handles the MetaSOY profile page.
 */
public class index extends MsoyEntryPoint
    implements HistoryListener
{
    // @Override
    public void onModuleLoad ()
    {
        super.onModuleLoad();

        History.addHistoryListener(this);
        String initToken = History.getToken();
        if (initToken.length() > 0) {
            onHistoryChanged(initToken);
        } else {
            // displaySummary();
        }

        RootPanel.get("content").add(new HeaderPanel());
    }

    // from interface HistoryListener
    public void onHistoryChanged (String token)
    {
        // TODO
    }
}
