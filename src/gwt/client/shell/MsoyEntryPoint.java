//
// $Id$

package client.shell;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.client.CatalogService;
import com.threerings.msoy.web.client.CatalogServiceAsync;
import com.threerings.msoy.web.client.GameService;
import com.threerings.msoy.web.client.GameServiceAsync;
import com.threerings.msoy.web.client.GroupService;
import com.threerings.msoy.web.client.GroupServiceAsync;
import com.threerings.msoy.web.client.ItemService;
import com.threerings.msoy.web.client.ItemServiceAsync;
import com.threerings.msoy.web.client.MailService;
import com.threerings.msoy.web.client.MailServiceAsync;
import com.threerings.msoy.web.client.MemberService;
import com.threerings.msoy.web.client.MemberServiceAsync;
import com.threerings.msoy.web.client.PersonService;
import com.threerings.msoy.web.client.PersonServiceAsync;
import com.threerings.msoy.web.client.ProfileService;
import com.threerings.msoy.web.client.ProfileServiceAsync;
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

        // create our client context
        _gctx = createContext();

        initContext();


        // create our standard navigation panel
        RootPanel.get("navigation").add(new NaviPanel(_gctx, getPageId()));

        // create our standard logon panel
        RootPanel.get("logon").add(_logon = new LogonPanel(_gctx, this));

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
     * Called during {@link #onModuleLoad} to initialize the application. Entry
     * points should override this method rather than onModuleLoad to set
     * themselves up. When this call returns, a call may immediately follow to
     * {@link #didLogon} if the user is already logged in, but the derived
     * class's onModuleLoad will <em>not</em> have been called yet.
     */
    protected abstract void onPageLoad ();

    /**
     * Creates the web context to be used by this page. Must extend {@link ShellContext}.
     */
    protected ShellContext createContext ()
    {
        return new ShellContext();
    }

    /**
     * Called after the context is created to initialize it. It is assumed that the derived class
     * maintained a casted reference to its context from @{link #createContext}.
     */
    protected void initContext ()
    {
        // wire up our remote services
        _gctx.usersvc = (WebUserServiceAsync)GWT.create(WebUserService.class);
        ((ServiceDefTarget)_gctx.usersvc).setServiceEntryPoint("/usersvc");
        _gctx.itemsvc = (ItemServiceAsync)GWT.create(ItemService.class);
        ((ServiceDefTarget)_gctx.itemsvc).setServiceEntryPoint("/itemsvc");
        _gctx.profilesvc = (ProfileServiceAsync)GWT.create(ProfileService.class);
        ((ServiceDefTarget)_gctx.profilesvc).setServiceEntryPoint("/profilesvc");
        _gctx.membersvc = (MemberServiceAsync)GWT.create(MemberService.class);
        ((ServiceDefTarget)_gctx.membersvc).setServiceEntryPoint("/membersvc");
        _gctx.personsvc = (PersonServiceAsync)GWT.create(PersonService.class);
        ((ServiceDefTarget)_gctx.personsvc).setServiceEntryPoint("/personsvc");
        _gctx.mailsvc = (MailServiceAsync)GWT.create(MailService.class);
        ((ServiceDefTarget)_gctx.mailsvc).setServiceEntryPoint("/mailsvc");
        _gctx.groupsvc = (GroupServiceAsync)GWT.create(GroupService.class);
        ((ServiceDefTarget)_gctx.groupsvc).setServiceEntryPoint("/groupsvc");
        _gctx.catalogsvc = (CatalogServiceAsync)GWT.create(CatalogService.class);
        ((ServiceDefTarget)_gctx.catalogsvc).setServiceEntryPoint("/catalogsvc");

        // load up our translation dictionaries
        _gctx.cmsgs = (ShellMessages)GWT.create(ShellMessages.class);
        _gctx.smsgs = (ServerMessages)GWT.create(ServerMessages.class);
    }

    /**
     * Clears out any existing content and sets the specified widget as the
     * main page content.
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
     * Called when we the player logs on after the page is loaded.
     */
    protected void didLogon (WebCreds creds)
    {
        // do nothing
    }

    /**
     * Called by our logon panel when the player logs on. You probably want to
     * override didLogon() instead.
     */
    protected void didLogon (WebCreds creds, boolean notify)
    {
        _gctx.creds = creds;
        if (notify) {
            didLogon(creds);
        }
    }

    /**
     * Called when the flash client has logged on.
     */
    protected void didLogonFromFlash (
        String displayName, int memberId, String token)
    {
        WebCreds creds = new WebCreds();
        creds.memberId = memberId;
        creds.token = token;
        _logon.didLogonFromFlash(displayName, creds);
    }

    /**
     * Called by our logon panel if the player logs off.
     */
    protected void didLogoff ()
    {
        _gctx.creds = null;
    }

    /**
     * Configure a top-level function (called flashDidLogon) that can be
     * called by flash to route logon information to didLogonFromFlash, above.
     */
    protected static native void configureLogonCallback (MsoyEntryPoint mep) /*-{
       $wnd.flashDidLogon = function (displayName, memberId, token) {
           mep.@client.shell.MsoyEntryPoint::didLogonFromFlash(Ljava/lang/String;ILjava/lang/String;)(displayName, memberId, token);
       };
    }-*/;

    protected ShellContext _gctx;
    protected LogonPanel _logon;

    protected HTML _chat;
}
