//
// $Id$

package client.world;

import client.shell.MsoyEntryPoint;
import client.util.FlashClients;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RootPanel;
import com.threerings.msoy.web.data.WebCreds;

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
        // don't show the flash client in the GWT shell
        if (!GWT.isScript()) {
            return;
        }
        try {
            if (token.startsWith("g")) {
                world("groupHome=" + id(token, 1));
                return;
            }
            if (token.startsWith("m")) {
                world("memberHome=" + id(token, 1));
                return;
            }
            if (token.startsWith("ng")) {
                // TODO: get group neighborhood, not member :)
                _ctx.membersvc.serializeNeighborhood(_ctx.creds, id(token, 2), new AsyncCallback() {
                    public void onSuccess (Object result) {
                        neighborhood((String) result);
                    }
                    public void onFailure (Throwable caught) {

                    }
                });
                return;
            }
            if (token.startsWith("nm")) {
                _ctx.membersvc.serializeNeighborhood(_ctx.creds, id(token, 2), new AsyncCallback() {
                    public void onSuccess (Object result) {
                        neighborhood((String) result);
                    }
                    public void onFailure (Throwable caught) {

                    }
                });
                return;
            }
        } catch (NumberFormatException e) {
            // fall through
        }
        world(null);
    }

    protected void neighborhood (String hood)
    {
        if (_client != null) {
            clientLogoff();
        }
        _client = FlashClients.createNeighborhood(hood);
        RootPanel.get("content").add(_client);
    }

    protected void world (String flashVar)
    {
        _client = FlashClients.createWorldClient(flashVar);
        RootPanel.get("content").add(_client);
    }

    protected int id (String token, int index)
    {
        return Integer.valueOf(token.substring(index)).intValue();
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
        clientLogon(creds.memberId, creds.token);
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
    protected static native void clientLogon (int memberId, String token) /*-{
        try {
            if ($doc.asclient) {
                $doc.asclient.clientLogon(memberId, token);

            } else if ($wnd.asclient) {
                $wnd.asclient.clientLogon(memberId, token);
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
                $doc.asclient.clientLogoff();
            } else if ($wnd.asclient) {
                $wnd.asclient.clientLogoff();
            }
        } catch (e) {
            // oh well
        }
    }-*/;

    protected HTML _client;
}
