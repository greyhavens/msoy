//
// $Id$

package client.profile;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;

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
    }

    // from interface HistoryListener
    public void onHistoryChanged (String token)
    {
        // TODO
    }
}
