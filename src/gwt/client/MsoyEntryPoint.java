//
// $Id$

package client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.client.ItemService;
import com.threerings.msoy.web.client.ItemServiceAsync;
import com.threerings.msoy.web.client.PersonService;
import com.threerings.msoy.web.client.PersonServiceAsync;
import com.threerings.msoy.web.client.ProfileService;
import com.threerings.msoy.web.client.ProfileServiceAsync;
import com.threerings.msoy.web.client.WebContext;
import com.threerings.msoy.web.client.WebUserService;
import com.threerings.msoy.web.client.WebUserServiceAsync;
import com.threerings.msoy.web.data.WebCreds;

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

    /**
     * Makes our media work both inside and out of the GWT development shell.
     */
    public static String toMediaPath (String path)
    {
        return GWT.isScript() ? path : "http://localhost:8080" + path;
    }

    // from interface EntryPoint
    public void onModuleLoad ()
    {
        // create our web context
        _ctx = new WebContext();
        _ctx.usersvc = (WebUserServiceAsync)GWT.create(WebUserService.class);
        ((ServiceDefTarget)_ctx.usersvc).setServiceEntryPoint("/user");
        _ctx.itemsvc = (ItemServiceAsync)GWT.create(ItemService.class);
        ((ServiceDefTarget)_ctx.itemsvc).setServiceEntryPoint("/item");
        _ctx.profilesvc = (ProfileServiceAsync)GWT.create(ProfileService.class);
        ((ServiceDefTarget)_ctx.profilesvc).setServiceEntryPoint("/profile");
        _ctx.personsvc = (PersonServiceAsync)GWT.create(PersonService.class);
        ((ServiceDefTarget)_ctx.personsvc).setServiceEntryPoint("/person");

        // create our standard logon panel
        RootPanel.get("logon").add(_logon = new LogonPanel(_ctx, this));

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
     * Clears out any existing content and sets the specified widget as the
     * main page content.
     */
    protected void setContent (Widget content)
    {
        RootPanel.get("content").clear();
        RootPanel.get("content").add(content);
    }

    /**
     * Called by our logon panel when the player logs on (or if we show up on
     * the page with valid credentials).
     */
    protected void didLogon (WebCreds creds)
    {
        _ctx.creds = creds;
    }

    /**
     * Called by our logon panel if the player logs off.
     */
    protected void didLogoff ()
    {
        _ctx.creds = null;
    }

    protected WebContext _ctx;
    protected LogonPanel _logon;
}
