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
    public static void display (String page, String token, String flashArgs)
    {
        // clear out our content and the expand/close controls
        RootPanel.get("content").clear();
        RootPanel.get("content").setWidth("0px");
        if (_controls.isAttached()) {
            RootPanel.get("client").remove(_controls);
        }

        // note that we need to hack our popups
        Page.needPopupHack = true;

        // create our client if necessary
        if (_client == null) {
            if (CShell.creds != null) {
                flashArgs = "token=" + CShell.creds.token +
                    (flashArgs == null ? "" : ("&" + flashArgs));;
            }
            RootPanel.get("client").clear();
            RootPanel.get("client").add(_client = FlashClients.createWorldClient(flashArgs));

        } else {
            // TODO: tell the client that it's not minimized
            clientGo(flashArgs);
        }

        // note our current page and history token
        _curPage = page;
        _curToken = token;
    }

    public static void minimize ()
    {
        // note that we don't need to hack our popups
        Page.needPopupHack = false;

        if (_client != null) {
            RootPanel.get("content").setWidth("724px");
            if (!_controls.isAttached()) {
                RootPanel.get("client").add(_controls);
            }
            // TODO: tell the client it's in minimized land
        } else {
            RootPanel.get("content").setWidth("100%");
        }
    }

    public static void clearClient ()
    {
        if (_client != null) {
            clientUnload();
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

    protected static class ControlPanel extends HorizontalPanel
    {
        public ControlPanel () {
            setSpacing(5);
            add(new Button("<<<", new ClickListener() {
                public void onClick (Widget sender) {
                    clearControls();
                    History.newItem(Application.createLinkToken(_curPage, _curToken));
                }
            }));
            add(new Button(">>>", new ClickListener() {
                public void onClick (Widget sender) {
                    clearClient();
                }
            }));
        }

        protected void clearControls () {
        }
    }

    protected static Widget _client;
    protected static ControlPanel _controls = new ControlPanel();
    protected static String _curPage, _curToken;
}
