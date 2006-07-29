//
// $Id$

package client;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;

/**
 * Handles the MetaSOY main page.
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
