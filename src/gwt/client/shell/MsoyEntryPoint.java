//
// $Id$

package client.shell;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.client.MemberService;
import com.threerings.msoy.web.client.MemberServiceAsync;
import com.threerings.msoy.web.client.WebUserService;
import com.threerings.msoy.web.client.WebUserServiceAsync;
import com.threerings.msoy.web.data.MemberName;
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
        return /* GWT.isScript() ? */ path /* : "http://localhost:8080" + path */;
    }

    /**
     * Returns a URL that displays the details of a given group.
     */
    public static String groupViewPath (int groupId)
    {
        return "/group/index.html#" + groupId;
    }

    /**
     * Returns a URL that displays the details of a given member.
     */
    public static String memberViewPath (int memberId)
    {
        return "/profile/index.html#" + memberId;
    }

    // from interface EntryPoint
    public void onModuleLoad ()
    {
        // set up the callbackd that our flash clients can call
        configureCallbacks(this);

        // initialize our services and translations
        initContext();

        // create our status/logon panel
        RootPanel.get("status").add(_status = new StatusPanel(this));

        // create our standard navigation panel
        RootPanel.get("navigation").add(_navi = new NaviPanel(getPageId(), _status));

        // initialize the status panel
        _status.init();

        // initialize the rest of the application
        onPageLoad();
    }

    /**
     * Returns the identifier of this page (used for navigation).
     */
    protected abstract String getPageId ();

    /**
     * Called during {@link #onModuleLoad} to initialize the application. Entry points should
     * override this method rather than onModuleLoad to set themselves up. A call will soon follow
     * to either {@link #didLogon} if the user is already logged in or {@link #didLogoff} if the
     * user is not logged in. Those methods should create the actual user interface.
     */
    protected abstract void onPageLoad ();

    /**
     * Called during initialization to give our entry point and derived classes a chance to
     * initialize their respective context classes.
     */
    protected void initContext ()
    {
        // wire up our remote services
        CShell.usersvc = (WebUserServiceAsync)GWT.create(WebUserService.class);
        ((ServiceDefTarget)CShell.usersvc).setServiceEntryPoint("/usersvc");
        CShell.membersvc = (MemberServiceAsync)GWT.create(MemberService.class);
        ((ServiceDefTarget)CShell.membersvc).setServiceEntryPoint("/membersvc");

        // load up our translation dictionaries
        CShell.cmsgs = (ShellMessages)GWT.create(ShellMessages.class);
        CShell.dmsgs = (DynamicMessages)GWT.create(DynamicMessages.class);
        CShell.smsgs = (ServerMessages)GWT.create(ServerMessages.class);
    }

    /**
     * Clears out any existing content and sets the specified widget as the main page content.
     */
    protected void setContent (Widget content)
    {
        RootPanel.get("content").clear();
        RootPanel.get("content").add(content);
    }

    /**
     * Called when we the player logs on (or when our session is validated). This always happens
     * after {@link #onPageLoad} as we don't know that our session is valid until we've heard back
     * from the server.
     *
     * @return true if we need a headless header Flash client, false if the page is providing a
     * Flash client for us.
     */
    protected boolean didLogon (WebCreds creds)
    {
        CShell.creds = creds;
        _navi.didLogon(creds);
        return true;
    }

    /**
     * Called when the flash client has logged on.
     */
    protected void didLogonFromFlash (String displayName, int memberId, String token)
    {
        _status.validateSession(token);
    }

    /**
     * Called by our logon panel if the player logs off.
     */
    protected void didLogoff ()
    {
        CShell.creds = null;
        _navi.didLogoff();
    }

    /**
     * Configures top-level functions that can be called by Flash when it wants to tell us about
     * things.
     */
    protected static native void configureCallbacks (MsoyEntryPoint mep) /*-{
       $wnd.flashDidLogon = function (displayName, memberId, token) {
           mep.@client.shell.MsoyEntryPoint::didLogonFromFlash(Ljava/lang/String;ILjava/lang/String;)(displayName, memberId, token);
       };
       $wnd.onunload = function (event) {
           var client = $doc.getElementById("asclient");
           if (client) {
               client.onUnload();
           }
           return true;
       };
    }-*/;

    protected NaviPanel _navi;
    protected StatusPanel _status;

    protected HTML _chat;
}
