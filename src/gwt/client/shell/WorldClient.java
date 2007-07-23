//
// $Id$

package client.shell;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.data.ConnectConfig;
import com.threerings.msoy.web.data.WebCreds;

import client.util.FlashClients;
import client.util.InfoPopup;

/**
 * Manages our World client (which also handles Flash games).
 */
public class WorldClient extends Widget
{
    public static void displayFlash (String flashArgs)
    {
        // if we have not yet determined our default server, find that out now
        if (_defaultServer == null) {
            final String savedArgs = flashArgs;
            CShell.usersvc.getConnectConfig(new AsyncCallback() {
                public void onSuccess (Object result) {
                    _defaultServer = (ConnectConfig)result;
                    displayFlash(savedArgs);
                }
                public void onFailure (Throwable cause) {
                    new InfoPopup(CShell.serverError(cause)).show();
                }
            });
            return;
        }

        // let the page know that we're displaying a client
        boolean newPage = Page.setShowingClient(true, false);

        // create our client if necessary
        if (! _isFlashClientPresent) {
            clearClient(false); // clear our Java client if we have one
            flashArgs += "&host=" + _defaultServer.server + "&port=" + _defaultServer.port;
            if (CShell.ident != null) {
                flashArgs += "&token=" + CShell.ident.token;
            }
            RootPanel.get("client").clear();
            FlashClients.embedWorldClient(RootPanel.get("client"), flashArgs);
            _isFlashClientPresent = true;
            
        } else {
            // don't tell the client anything if we're just restoring our URL
            if (newPage) {
                clientGo(flashArgs);
            }
            clientMinimized(false);
        }
    }

    public static void displayJava (Widget client)
    {
        // let the page know that we're displaying a client
        boolean newPage = Page.setShowingClient(false, true);

        if (_jclient != client) {
            if (newPage) {
                clearClient(false); // clear out our flash client if we have one
                RootPanel.get("client").clear();
                RootPanel.get("client").add(_jclient = client);
            }
        } else {
            clientMinimized(false);
        }
    }

    public static void minimize ()
    {
        // note that we don't need to hack our popups
        Page.displayingFlash = false;

        if (_isFlashClientPresent || _jclient != null) {
            RootPanel.get("client").setWidth("300px");
            clientMinimized(true);
        }
    }

    public static void clearClient (boolean restoreContent)
    {
        if (_isFlashClientPresent || _jclient != null) {
            if (_isFlashClientPresent) {
                clientUnload(); // TODO: make this work for jclient
            }
            RootPanel.get("client").clear();
            _isFlashClientPresent = false;
            _jclient = null;
        }
        if (restoreContent) {
            RootPanel.get("client").setWidth("0px");
            RootPanel.get("content").setWidth("100%");
        }
    }

    public static void didLogon (WebCreds creds)
    {
        if (_isFlashClientPresent) {
            clientLogon(creds.getMemberId(), creds.token);
        }
        // TODO: let jclient know about logon?
    }

    public static void didLogoff ()
    {
        clearClient(true);
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
    protected static native void clientUnload () /*-{
        var client = $doc.getElementById("asclient");
        if (client) {
            client.onUnload();
        }
    }-*/;

    /**
     * Notifies the flash client that we're either minimized or not.
     */
    protected static native void clientMinimized (boolean mini) /*-{
        var client = $doc.getElementById("asclient");
        if (client) {
            client.setMinimized(mini);
        }
    }-*/;

    protected static boolean _isFlashClientPresent;
    protected static Widget _jclient;

    /** Our default world server. Configured the first time Flash is used. */
    protected static ConnectConfig _defaultServer;
}
