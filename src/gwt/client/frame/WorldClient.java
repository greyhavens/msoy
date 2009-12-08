//
// $Id$

package client.frame;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Panel;

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
 * Manages a World client (which also handles Flash games).
 */
public class WorldClient
{
    public interface PanelProvider
    {
        public Panel get ();
    }

    public void setDefaultServer (String host, int port)
    {
        _defaultHost = host;
        _defaultPort = port;
    }

    public void displayFlash (String flashArgs, final PanelProvider pprov)
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
        if (_flashPanel != null && clientGo(flashArgs)) {
            _flashArgs = flashArgs; // note our new current flash args
            setMinimized(false); // TODO: why is this here?

        } else {
            // flash is not resolved or it's hosed, create or recreate the client
            embedClient(flashArgs, pprov.get());
        }
    }

    /**
     * Called when the Flash client has determined that it's hosed (lost connection or otherwise
     * died) and wants to be thrown away and recreated at its present location.
     */
    public void rebootFlash (PanelProvider pprov)
    {
        embedClient(_flashArgs, pprov.get());
    }

    public void setMinimized (boolean minimized)
    {
        _minimized = minimized;
        clientMinimized(minimized);
    }

    public void setChromeless ()
    {
        _chromeless = true;
    }

    /**
     * Lets the world client know that the user has navigated to the specified content page.
     */
    public void contentRequested (Pages page, String token)
    {
        // no need to pass this along right now
    }

    /**
     * Lets the world client know that the content page has now loaded and initialized.
     */
    public void contentPageReady (Pages page, String token)
    {
        clientSetPage(page.name(), token);
    }

    /**
     * Lets the world client know that the user has closed the GWT content.
     */
    public void contentCleared ()
    {
        clientSetPage(null, null);
    }

    public void clientWillClose ()
    {
        if (_flashPanel != null) {
            clientUnload();
            _flashArgs = null;
            _flashPanel = null;
        }
    }

    public void didLogon (WebCreds creds)
    {
        if (_flashPanel != null) {
            clientLogon(creds.getMemberId(), creds.token);
        }
        // TODO: propagate creds to our flash SharedObject in case next login is from an embed?
    }

    protected void embedClient (String flashArgs, Panel parent)
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
        if (_chromeless) {
            flashArgs += "&chromeless=true";
        }
        String affstr = CookieUtil.get(CookieNames.AFFILIATE);
        if (!StringUtil.isBlank(affstr)) {
            flashArgs += "&aff=" + affstr;
        }

        parent.clear();
        FlashClients.embedWorldClient(parent, flashArgs);
    }

    /**
     * Tells the World client to go to a particular location.
     */
    protected native boolean clientGo (String where) /*-{
        var client = @client.util.FlashClients::findClient()();
        if (client) {
            // exceptions from JavaScript break GWT; don't let that happen
            try { return client.clientGo(where); } catch (e) {}
        }
        return false;
    }-*/;

    /**
     * Logs on the MetaSOY Flash client using magical JavaScript.
     */
    protected native void clientLogon (int memberId, String token) /*-{
        var client = @client.util.FlashClients::findClient()();
        if (client) {
            // exceptions from JavaScript break GWT; don't let that happen
            try { client.clientLogon(memberId, token); } catch (e) {}
        }
    }-*/;

    /**
     * Logs off the MetaSOY Flash client using magical JavaScript.
     */
    protected native void clientUnload () /*-{
        var client = @client.util.FlashClients::findClient()();
        if (client) {
            // exceptions from JavaScript break GWT; don't let that happen
            try { client.onUnload(); } catch (e) {}
        }
    }-*/;

    /**
     * Notifies the flash client that we're either minimized or not.
     */
    protected native void clientMinimized (boolean mini) /*-{
        var client = @client.util.FlashClients::findClient()();
        if (client) {
            // exceptions from JavaScript break GWT; don't let that happen
            try { client.setMinimized(mini); } catch (e) {}
        }
    }-*/;

    /**
     * Notifies the flash client of the page and token we are viewing.
     */
    protected native void clientSetPage (String page, String token) /*-{
        var client = @client.util.FlashClients::findClient()();
        if (client) {
            // exceptions from JavaScript break GWT; don't let that happen
            try { client.setPage(page, token); } catch (e) {}
        }
    }-*/;

    protected String _flashArgs;
    protected Panel  _flashPanel;
    protected boolean _minimized;

    /** Whether or not the client is in chromeless mode. */
    protected boolean _chromeless;

    /** Our default world server host and port. Configured the first time Flash is used. */
    protected String _defaultHost;
    protected int _defaultPort;

    protected static final WebUserServiceAsync _usersvc = GWT.create(WebUserService.class);
}
