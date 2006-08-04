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
        RootPanel.get("content").add(_header = new HeaderPanel());

        History.addHistoryListener(this);
        String initToken = History.getToken();
        if (initToken.length() > 0) {
            onHistoryChanged(initToken);
        } else if (_ctx.creds != null) {
            displayProfile(_ctx.creds.memberId);
        }
    }

    // from interface HistoryListener
    public void onHistoryChanged (String token)
    {
        // parse the token as a memberId and display their profile
        try {
            displayProfile(Integer.parseInt(token));
        } catch (Exception e) {
            // TODO: display "no known profile for that user"
        }
    }

    protected void displayProfile (int memberId)
    {
    }

    protected HeaderPanel _header;
}
