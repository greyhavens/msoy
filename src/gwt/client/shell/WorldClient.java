//
// $Id$

package client.shell;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.data.WebCreds;

import client.util.FlashClients;

/**
 * Manages our World client (which also handles Flash games).
 */
public class WorldClient extends Widget
{
    public static void display (String args)
    {
        // clear out our content
        RootPanel.get("content").clear();

        // create our client if necessary
        if (_client == null) {
            if (CShell.creds != null) {
                args = "token=" + CShell.creds.token + (args == null ? "" : ("&" + args));;
            }
            RootPanel.get("client").clear();
            RootPanel.get("client").add(_client = FlashClients.createWorldClient(args));

        } else {
            // TODO: tell the client that it's not minimized
            clientGo(args);
        }

        // widen that bad boy on up; we're now the star of the show
        RootPanel.get("content").setWidth("0px");
    }

    public static void minimize ()
    {
        if (_client != null) {
            RootPanel.get("content").setWidth("724px");
            // TODO: tell the client it's in minimized land
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
        if (_client != null) {
            clientLogoff();
        }
    }

    /**
     * Tells the World client to go to a particular location.
     */
    protected static native boolean clientGo (String where) /*-{
        var client = $doc.getElementById("asclient");
        if (client) {
            client.clientGo(where);
            return true;
        }
        return false;
    }-*/;

    /**
     * Logs on the MetaSOY Flash client using magical JavaScript.
     */
    protected static native void clientLogon (int memberId, String token) /*-{
        var client = $doc.getElementById("asclient");
        if (client) {
            client.clientLogon(memberId, token);
        }
    }-*/;

    /**
     * Logs off the MetaSOY Flash client using magical JavaScript.
     */
    protected static native void clientLogoff () /*-{
        var client = $doc.getElementById("asclient");
        if (client) {
            client.clientLogoff();
        }
    }-*/;

    protected static Widget _client;
}
