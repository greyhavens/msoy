//
// $Id$

package client;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;

import client.util.FlashWidget;

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
            onHistoryChanged("home");
        }
    }

    // from interface HistoryListener
    public void onHistoryChanged (String token)
    {
        RootPanel.get("content").clear();

        if ("home".equals(token)) {
            FlashWidget client = new FlashWidget("client");
            client.setMovie("/clients/Msoy.swf");
            client.setSize(900, 600);
            RootPanel.get("content").add(client);
        } else {
            RootPanel.get("content").add(new Label("Unknown page: " + token));
        }
    }
}
