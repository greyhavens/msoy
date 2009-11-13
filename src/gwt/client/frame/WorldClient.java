//
// $Id$

package client.frame;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.util.CookieUtil;
import com.threerings.gwt.util.StringUtil;

import com.threerings.msoy.web.gwt.ConnectConfig;
import com.threerings.msoy.web.gwt.CookieNames;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.WebCreds;
import com.threerings.msoy.web.gwt.WebUserService;
import com.threerings.msoy.web.gwt.WebUserServiceAsync;

import client.shell.CShell;
import client.util.FlashClients;
import client.util.InfoCallback;

/**
 * Manages our World client (which also handles Flash games).
 */
public class WorldClient extends Widget
{
    public static interface PanelProvider
    {
        public Panel get ();
    }

    public static void setDefaultServer (String host, int port)
    {
        _defaultHost = host;
        _defaultPort = port;
    }

    public static void displayFlash (String flashArgs, final PanelProvider pprov)
    {
        // if we have not yet determined our default server, find that out now
        if (_defaultHost == null) {
            final String savedArgs = flashArgs;
            _usersvc.getConnectConfig(new InfoCallback<ConnectConfig>() {
                public void onSuccess (ConnectConfig config) {
                    _defaultHost = config.server;
                    _defaultPort = config.port;
                    displayFlash(savedArgs, pprov);
                }
            });
            return;
        }

        // if we're currently already displaying exactly what we've been asked to display; then
        // stop here because we're just restoring our client after closing a GWT page
        if (flashArgs.equals(_flashArgs)) {
            return;
        }

        // create our client if necessary
        if (_flashPanel != null && clientGo("asclient", flashArgs)) {
            _flashArgs = flashArgs; // note our new current flash args
            setMinimized(false);

        } else {
            // flash is not resolved or it's hosed, create or recreate the client
            embedClient(flashArgs, pprov.get());
        }
    }

    /**
     * Called when the Flash client has determined that it's hosed (lost connection or otherwise
     * died) and wants to be thrown away and recreated at its present location.
     */
    public static void rebootFlash (PanelProvider pprov)
    {
        embedClient(_flashArgs, pprov.get());
    }

    public static void displayJava (Widget client, PanelProvider pprov)
    {
        // clear out any flash page stuff
        _flashArgs = null;
        _flashPanel = null;

        if (_javaPanel != client) {
            clientWillClose(); // clear out our flash client if we have one
            pprov.get().add(_javaPanel = client);
        } else {
            setMinimized(false);
        }
    }

    public static void setMinimized (boolean minimized)
    {
        _minimized = minimized;
        clientMinimized(minimized);
    }

    /**
     * Lets the world client know that the user has navigated to the specified content page.
     */
    public static void contentChanged (Pages page, String token)
    {
        clientSetPage(page.getPath(), token);
    }

    /**
     * Lets the world client know that the user has closed the GWT content.
     */
    public static void contentCleared ()
    {
        clientSetPage(null, null);
    }

    public static void clientWillClose ()
    {
        if (_flashPanel != null || _javaPanel != null) {
            if (_flashPanel != null) {
                clientUnload(); // TODO: make this work for jclient
            }
            _flashArgs = null;
            _flashPanel = null;
            _javaPanel = null;
        }
    }

    public static void didLogon (WebCreds creds)
    {
        if (_flashPanel != null) {
            clientLogon(creds.getMemberId(), creds.token);
        }
        // TODO: let jclient know about logon?
        // TODO: propagate creds to our flash SharedObject in case next login is from an embed?
    }

    protected static void embedClient (String flashArgs, Panel parent)
    {
        clientWillClose(); // clear our clients if we have any

        _flashPanel = parent;
        _flashArgs = flashArgs;

        // augment the arguments with things that are only relevant to the initial embed, i.e. not
        // logically part of the location of the client
        if (flashArgs.indexOf("&host") == -1) {
            flashArgs += "&host=" + _defaultHost;
        }
        if (flashArgs.indexOf("&port") == -1) {
            flashArgs += "&port=" + _defaultPort;
        }
        if (CShell.getAuthToken() != null) {
            flashArgs += "&token=" + CShell.getAuthToken();
        }
        if (_minimized) {
            flashArgs += "&minimized=t";
        }
        flashArgs += getAffiliateArg();

        parent.clear();
        FlashClients.embedWorldClient(parent, flashArgs);
    }

    protected static String getAffiliateArg ()
    {
        String affstr = CookieUtil.get(CookieNames.AFFILIATE);
        return StringUtil.isBlank(affstr) ? "" : ("&aff=" + affstr);
    }

    /**
     * Tells the World client to go to a particular location.
     */
    protected static native boolean clientGo (String id, String where) /*-{
        var client = $doc.getElementById(id);
        if (client) {
            // exceptions from JavaScript break GWT; don't let that happen
            try { return client.clientGo(where); } catch (e) {}
        }
        return false;
    }-*/;

    /**
     * Logs on the MetaSOY Flash client using magical JavaScript.
     */
    protected static native void clientLogon (int memberId, String token) /*-{
        var client = $doc.getElementById("asclient");
        if (client) {
            // exceptions from JavaScript break GWT; don't let that happen
            try { client.clientLogon(memberId, token); } catch (e) {}
        }
    }-*/;

    /**
     * Logs off the MetaSOY Flash client using magical JavaScript.
     */
    protected static native void clientUnload () /*-{
        var client = $doc.getElementById("asclient");
        if (client) {
            // exceptions from JavaScript break GWT; don't let that happen
            try { client.onUnload(); } catch (e) {}
        }
    }-*/;

    /**
     * Notifies the flash client that we're either minimized or not.
     */
    protected static native void clientMinimized (boolean mini) /*-{
        var client = $doc.getElementById("asclient");
        if (client) {
            // exceptions from JavaScript break GWT; don't let that happen
            try { client.setMinimized(mini); } catch (e) {}
        }
    }-*/;

    /**
     * Notifies the flash client of the page and token we are viewing.
     */
    protected static native void clientSetPage (String page, String token) /*-{
        var client = $doc.getElementById("asclient");
        if (client) {
            // exceptions from JavaScript break GWT; don't let that happen
            try { client.setPage(page, token); } catch (e) {}
        }
    }-*/;

    protected static String _flashArgs;
    protected static Panel  _flashPanel;
    protected static Widget _javaPanel;
    protected static boolean _minimized;

    /** Our default world server host and port. Configured the first time Flash is used. */
    protected static String _defaultHost;
    protected static int _defaultPort;

    protected static final WebUserServiceAsync _usersvc = GWT.create(WebUserService.class);
}
