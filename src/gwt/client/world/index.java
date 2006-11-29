//
// $Id$

package client.world;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.web.data.WebCreds;

import client.shell.MsoyEntryPoint;

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

    // from interface HistoryListener
    public void onHistoryChanged (String token)
    {
        RootPanel.get("content").clear();

        if ("home".equals(token)) {
            // don't show the flash client in the GWT shell
            if (GWT.isScript()) {
                if (_client == null) {
                    _client = WidgetUtil.createFlashContainer(
                        // TODO: fix height arg to be 100% (doesn't currently work)
                        "asclient", "/clients/game-client.swf", "100%", "550", null);
                }
                RootPanel.get("content").add(_client);
            }

        } else {
            // if we have a client around, log if off
            if (_client != null) {
                clientLogoff();
                _client = null;
            }

            RootPanel.get("content").add(new Label("Unknown page: " + token));
        }
    }

    // @Override // from MsoyEntryPoint
    protected String getPageId ()
    {
        return "index";
    }

    // @Override // from MsoyEntryPoint
    protected void onPageLoad ()
    {
        History.addHistoryListener(this);
        String initToken = History.getToken();
        if (initToken.length() > 0) {
            onHistoryChanged(initToken);
        } else {
            onHistoryChanged("home");
        }
    }

    // @Override // from MsoyEntryPoint
    protected void didLogon (WebCreds creds)
    {
        super.didLogon(creds);
        clientLogon(creds.token);
    }

    // @Override // from MsoyEntryPoint
    protected void didLogoff ()
    {
        super.didLogoff();
        clientLogoff();
    }

    /**
     * Logs on the MetaSOY Flash client using magical JavaScript.
     */
    protected static native void clientLogon (String token) /*-{
        try {
            if ($doc.asclient) {
                $doc.asclient.logon(token);
            } else if ($wnd.asclient) {
                $wnd.asclient.logon(token);
            }
        } catch (e) {
            // oh well
        }
    }-*/;

    /**
     * Logs off the MetaSOY Flash client using magical JavaScript.
     */
    protected static native void clientLogoff () /*-{
        try {
            if ($doc.asclient) {
                $doc.asclient.logoff();
            } else if ($wnd.asclient) {
                $wnd.asclient.logoff();
            }
        } catch (e) {
            // oh well
        }
    }-*/;

    protected HTML _client;
}
