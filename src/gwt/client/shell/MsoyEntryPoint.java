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
        // First, set up the callback that flash can call when it logs in
        configureLogonCallback(this);

        // initialize our services and translations
        initContext();

        // TODO: new image every day!
//         RootPanel.get("logo").add(new Image("/images/header/msoy_logo.png"));

        // create our standard logon panel
        RootPanel.get("status").add(_logon = new LogonPanel(this));

        // create our standard navigation panel
        RootPanel.get("navigation").add(_navi = new NaviPanel(getPageId(), _logon));

        // initialize the logon panel
        _logon.init();

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
        CShell.smsgs = (ServerMessages)GWT.create(ServerMessages.class);
    }

    /**
     * Clears out any existing content and sets the specified widget as the main page content.
     */
    protected void setContent (Widget content)
    {
        RootPanel.get("content").clear();
        String id = getPageId();
//         if ((id != "world") && (id != "game")) {
//             if (_chat == null) {
//                 _chat = FlashClients.createChatClient();
//             }
//             RootPanel.get("content").add(_chat);
//         }
        RootPanel.get("content").add(content);
    }

    /**
     * Called when we the player logs on (or when our session is validated). This always happens
     * after {@link #onPageLoad} as we don't know that our session is valid until we've heard back
     * from the server.
     */
    protected void didLogon (WebCreds creds)
    {
        CShell.creds = creds;
        _navi.didLogon(creds);
    }

    /**
     * Called when the flash client has logged on.
     */
    protected void didLogonFromFlash (String displayName, int memberId, String token)
    {
        _logon.validateSession(token);
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
     * Configure a top-level function (called flashDidLogon) that can be called by flash to route
     * logon information to didLogonFromFlash, above.
     */
    protected static native void configureLogonCallback (MsoyEntryPoint mep) /*-{
       $wnd.flashDidLogon = function (displayName, memberId, token) {
           mep.@client.shell.MsoyEntryPoint::didLogonFromFlash(Ljava/lang/String;ILjava/lang/String;)(displayName, memberId, token);
       };
    }-*/;

    protected NaviPanel _navi;
    protected LogonPanel _logon;

    protected HTML _chat;
}
