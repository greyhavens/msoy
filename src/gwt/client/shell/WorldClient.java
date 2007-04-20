//
// $Id$

package client.shell;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.data.WebCreds;

import client.util.FlashClients;

/**
 * Manages our World client (which also handles Flash games).
 */
public class WorldClient extends Widget
{
    public static void display (String flashArgs)
    {
        // clear out our content and the expand/close controls
        RootPanel.get("content").clear();
        RootPanel.get("content").setWidth("0px");

        // note that we need to hack our popups
        Page.displayingFlash = true;

        // create our client if necessary
        if (_client == null) {
            if (CShell.creds != null) {
                flashArgs = "token=" + CShell.creds.token +
                    (flashArgs == null ? "" : ("&" + flashArgs));;
            }
            RootPanel.get("client").clear();
            RootPanel.get("client").add(_client = FlashClients.createWorldClient(flashArgs));

        } else {
            clientGo(flashArgs);
            clientMinimized(false);
        }

        // have the client take up all the space
        RootPanel.get("client").setWidth("100%");
    }

    public static void minimize ()
    {
        // note that we don't need to hack our popups
        Page.displayingFlash = false;

        if (_client != null) {
            clientMinimized(true);
            RootPanel.get("client").setWidth("300px");
        }
    }

    public static void clearClient ()
    {
        if (_client != null) {
            clientUnload();
            RootPanel.get("client").setWidth("0px");
            RootPanel.get("client").clear();
            _client = null;
            RootPanel.get("content").setWidth("100%");
        }
    }

    public static void didLogon (WebCreds creds)
    {
        if (_client != null) {
            clientLogon(creds.getMemberId(), creds.token);
        }
    }

    public static void didLogoff ()
    {
        clearClient();
    }

    /**
     * Tells the World client to go to a particular location.
     */
    protected static native boolean clientGo (String where) /*-{
        var client = $doc.getElementById("asclient");
        if (client) {
            try {
                client.clientGo(where);
                return true;
            } catch (e) {
                // nada
            }
        }
        return false;
    }-*/;

    /**
     * Logs on the MetaSOY Flash client using magical JavaScript.
     */
    protected static native void clientLogon (int memberId, String token) /*-{
        var client = $doc.getElementById("asclient");
        if (client) {
            try {
                client.clientLogon(memberId, token);
            } catch (e) {
                // nada
            }
        }
    }-*/;

    /**
     * Logs off the MetaSOY Flash client using magical JavaScript.
     */
    protected static native void clientUnload () /*-{
        var client = $doc.getElementById("asclient");
        if (client) {
            try {
                client.onUnload();
            } catch (e) {
                // nada
            }
        }
    }-*/;

    /**
     * notifies the flash client that we're either minimized or not.
     */
    protected static native void clientMinimized (boolean mini) /*-{
        var client = $doc.getElementById("asclient");
        if (client) {
            try {
                client.setMinimized(mini);
            } catch (e) {
                // nada
            }
        }
    }-*/;

    protected static Widget _client;
}
