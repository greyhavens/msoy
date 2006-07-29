//
// $Id$

package client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

import com.google.gwt.user.client.ui.RootPanel;

import com.threerings.msoy.web.client.WebCreds;
import com.threerings.msoy.web.client.WebUserService;
import com.threerings.msoy.web.client.WebUserServiceAsync;

/**
 * Handles some standard services for a top-level MetaSOY web application
 * "entry point".
 */
public abstract class MsoyEntryPoint
    implements EntryPoint
{
    /** Used to dynamically create the appropriate entry point depending on
     * which page via which our module was loaded. */
    public static interface Creator {
        public MsoyEntryPoint createEntryPoint ();
    }

    // from interface EntryPoint
    public void onModuleLoad ()
    {
        // get access to our service
        _usersvc = (WebUserServiceAsync)GWT.create(WebUserService.class);
        ServiceDefTarget target = (ServiceDefTarget)_usersvc;
        target.setServiceEntryPoint("/user");

        // create our standard logon panel
        RootPanel.get("logon").add(_logon = new LogonPanel(this, _usersvc));

        // initialize the rest of the application
        onPageLoad();

        // and now potentially trigger a call to didLogon()
        _logon.init();
    }

    /**
     * Called during {@link #onModuleLoad} to initialize the application. Entry
     * points should override this method rather than onModuleLoad to set
     * themselves up. When this call returns, a call may immediately follow to
     * {@link #didLogon} if the user is already logged in, but the derived
     * class's onModuleLoad will <em>not</em> have been called yet.
     */
    protected abstract void onPageLoad ();

    /**
     * Called by our logon panel when the player logs on (or if we show up on
     * the page with valid credentials).
     */
    protected void didLogon (WebCreds creds)
    {
    }

    /**
     * Called by our logon panel if the player logs off.
     */
    protected void didLogoff ()
    {
    }

    protected WebUserServiceAsync _usersvc;
    protected LogonPanel _logon;
}
