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

import client.util.FlashClients;
import client.util.WebContext;

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

        // create our web context
        _ctx = new WebContext();

        String prefix = /* GWT.isScript() ? */ "/" /* : GWT.getModuleBaseURL() */;
        _ctx.usersvc = (WebUserServiceAsync)GWT.create(WebUserService.class);
        ((ServiceDefTarget)_ctx.usersvc).setServiceEntryPoint(prefix + "usersvc");
        _ctx.itemsvc = (ItemServiceAsync)GWT.create(ItemService.class);
        ((ServiceDefTarget)_ctx.itemsvc).setServiceEntryPoint(prefix + "itemsvc");
        _ctx.profilesvc = (ProfileServiceAsync)GWT.create(ProfileService.class);
        ((ServiceDefTarget)_ctx.profilesvc).setServiceEntryPoint(prefix + "profilesvc");
        _ctx.membersvc = (MemberServiceAsync)GWT.create(MemberService.class);
        ((ServiceDefTarget)_ctx.membersvc).setServiceEntryPoint(prefix + "membersvc");
        _ctx.personsvc = (PersonServiceAsync)GWT.create(PersonService.class);
        ((ServiceDefTarget)_ctx.personsvc).setServiceEntryPoint(prefix + "personsvc");
        _ctx.mailsvc = (MailServiceAsync)GWT.create(MailService.class);
        ((ServiceDefTarget)_ctx.mailsvc).setServiceEntryPoint(prefix + "mailsvc");
        _ctx.groupsvc = (GroupServiceAsync)GWT.create(GroupService.class);
        ((ServiceDefTarget)_ctx.groupsvc).setServiceEntryPoint(prefix + "groupsvc");
        _ctx.catalogsvc = (CatalogServiceAsync)GWT.create(CatalogService.class);
        ((ServiceDefTarget)_ctx.catalogsvc).setServiceEntryPoint(prefix + "catalogsvc");
        _ctx.gamesvc = (GameServiceAsync)GWT.create(GameService.class);
        ((ServiceDefTarget)_ctx.gamesvc).setServiceEntryPoint(prefix + "gamesvc");


        // create our standard navigation panel
        RootPanel.get("navigation").add(new NaviPanel(_ctx, getPageId()));

        // create our standard logon panel
        RootPanel.get("logon").add(_logon = new LogonPanel(_ctx, this));

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
        _ctx.creds = creds;
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
        _ctx.creds = null;
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

    protected WebContext _ctx;
    protected LogonPanel _logon;

    protected HTML _chat;
}
