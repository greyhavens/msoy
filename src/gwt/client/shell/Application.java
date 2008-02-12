//
// $Id$

package client.shell;

import java.util.HashMap;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Label;

import com.threerings.gwt.util.CookieUtil;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.web.client.CatalogService;
import com.threerings.msoy.web.client.CatalogServiceAsync;
import com.threerings.msoy.web.client.CommentService;
import com.threerings.msoy.web.client.CommentServiceAsync;
import com.threerings.msoy.web.client.DeploymentConfig;
import com.threerings.msoy.web.client.ItemService;
import com.threerings.msoy.web.client.ItemServiceAsync;
import com.threerings.msoy.web.client.MemberService;
import com.threerings.msoy.web.client.MemberServiceAsync;
import com.threerings.msoy.web.client.WebUserService;
import com.threerings.msoy.web.client.WebUserServiceAsync;
import com.threerings.msoy.web.data.Invitation;
import com.threerings.msoy.web.data.SessionData;
import com.threerings.msoy.web.data.WebCreds;
import com.threerings.msoy.web.data.WebIdent;

import client.editem.EditemMessages;
import client.item.ItemMessages;

/**
 * Our main application and entry point. This dispatches a requests to the appropriate {@link
 * Page}. Some day it may also do fancy on-demand loading of JavaScript.
 */
public class Application
    implements EntryPoint, HistoryListener
{
    /** The height of our header (including the black bar with location label) in pixels. */
    public static final int HEADER_HEIGHT = 50 /* header */ + 20 /* location bar */;

    /** Our active invitation if we landed at Whirled from an invite, null otherwise. */
    public static Invitation activeInvite;

    /**
     * Returns a {@link Hyperlink} that displays the details of a given group.
     */
    public static Hyperlink groupViewLink (String label, int groupId)
    {
        return createLink(label, Page.WHIRLEDS, ""+groupId);
    }

    /**
     * Returns a {@link Hyperlink} that displays the details of a given member.
     */
    public static Hyperlink memberViewLink (String label, int memberId)
    {
        return createLink(label, Page.PEOPLE, ""+memberId);
    }

    /**
     * Returns a {@link Hyperlink} that displays the details of a given member.
     */
    public static Hyperlink memberViewLink (MemberName name)
    {
        return createLink(name.toString(), Page.PEOPLE, ""+name.getMemberId());
    }

    /**
     * Returns a {@link Hyperlink} that navigates to the specified application page with the
     * specified arguments. A page should use this method to pass itself arguments.
     */
    public static Hyperlink createLink (String label, String page, String args)
    {
        Hyperlink link = new Hyperlink(label, createLinkToken(page, args));
        link.addStyleName("inline");
        return link;
    }

    /**
     * Returns HTML that links to the specified page with the specified arguments. Don't use this
     * if you can avoid it. Hyperlink does special stuff to make the history mechanism work in some
     * browsers and this breaks that.
     */
    public static String createLinkHtml (String label, String page, String args)
    {
        return "<a href=\"#" + createLinkToken(page, args) + "\">" + label + "</a>";
    }

    /**
     * Move to the page in question.
     */
    public static void go (String page, String args)
    {
        String token = createLinkToken(page, args);
        if (token.equals(History.getToken())) {
            Frame.setContentMinimized(false, null);
        } else {
            History.newItem(token);
        }
    }

    /**
     * Returns a string that can be appended to '#' to link to the specified page with the
     * specified arguments.
     */
    public static String createLinkToken (String page, String args)
    {
        String token = page;
        if (args != null && args.length() > 0) {
            token = token + "-" + args;
        }
        return token;
    }

    /**
     * Configures our current history token (normally this is done automatically as the user
     * navigates, but sometimes we want to override the current token). This does not take any
     * action based on the token, but the token will be used if the user subsequently logs in or
     * out.
     */
    public static void setCurrentToken (String token)
    {
        _currentToken = token;
    }

    /**
     * Returns a partner identifier when we're running in partner cobrand mode, null when we're
     * running in the full Whirled environment.
     */
    public static native String getPartner () /*-{
        return $doc.whirledPartner;
    }-*/;

    // from interface EntryPoint
    public void onModuleLoad ()
    {
        // create our static page mappings (we can't load classes by name in wacky JavaScript land
        // so we have to hardcode the mappings)
        createMappings();

        // initialize our top-level context references
        initContext();

        // set up the callbackd that our flash clients can call
        configureCallbacks(this);

        // create our status panel and initialize the frame
        _status = new StatusPanel(this);
        Frame.init();

        // wire ourselves up to the history-based navigation mechanism
        History.addHistoryListener(this);
        _currentToken = History.getToken();

        // validate our session before considering ourselves logged on
        validateSession(CookieUtil.get("creds"));
    }

    // from interface HistoryListener
    public void onHistoryChanged (String token)
    {
        _currentToken = token;

        String page = (token == null || token.equals("")) ? Page.ME : token;
        Args args = new Args();
        int dashidx = token.indexOf("-");
        if (dashidx != -1) {
            page = token.substring(0, dashidx);
            args.setToken(token.substring(dashidx+1));
        }

        // TEMP: migrate old style invites to new style
        if ("invite".equals(page)) {
            token = Args.compose("i", args.get(0, ""));
            args = new Args();
            args.setToken(token);
            page = Page.ME;
        } else if ("optout".equals(page) || "resetpw".equals(page)) {
            token = Args.compose(page, args.get(0, ""), args.get(1, ""));
            args = new Args();
            args.setToken(token);
            page = Page.ACCOUNT;
        } else if (Page.WORLD.equals(page) && args.get(0, "").equals("i")) {
            page = Page.ME;
        }
        // END TEMP

        CShell.log("Displaying page [page=" + page + ", args=" + args + "].");

        // replace the page if necessary
        if (_page == null || !_page.getPageId().equals(page)) {
            // tell any existing page that it's being unloaded
            if (_page != null) {
                _page.onPageUnload();
                _page = null;
            }

            // locate the creator for this page
            Page.Creator creator = (Page.Creator)_creators.get(page);
            if (creator == null) {
                CShell.log("Page unknown, redirecting to me [page=" + page + "].");
                creator = (Page.Creator)_creators.get(Page.ME);
                args = new Args();
            }

            // create the entry point and fire it up
            _page = creator.createPage();
            _page.init();
            _page.onPageLoad();

            // tell the page about its arguments
            _page.onHistoryChanged(args);

        } else {
            final Args fargs = args;
            Frame.setContentMinimized(false, new Command() {
                public void execute () {
                    // now tell the page about its arguments
                    _page.onHistoryChanged(fargs);
                }
            });
        }
    }

    /**
     * Returns a reference to the status panel.
     */
    public StatusPanel getStatusPanel ()
    {
        return _status;
    }

    /**
     * Called when the player logs on (or when our session is validated).
     */
    public void didLogon (SessionData data)
    {
        CShell.creds = data.creds;
        CShell.ident = new WebIdent(data.creds.getMemberId(), data.creds.token);
        _status.didLogon(data);
        WorldClient.didLogon(data.creds);
        Frame.didLogon();

        if (_page != null) {
            _page.didLogon(data.creds);
        } else if (_currentToken != null) {
            onHistoryChanged(_currentToken);
        }
    }

    /**
     * Called when the player logs off.
     */
    public void didLogoff ()
    {
        CShell.creds = null;
        CShell.ident = null;
        _status.didLogoff();

        if (_page == null) {
            // we can now load our starting page
            onHistoryChanged(_currentToken);
        } else {
            Frame.closeClient(false);
            _page.didLogoff();
        }
    }

    protected void initContext ()
    {
        CShell.app = this;

        // wire up our remote services
        CShell.usersvc = (WebUserServiceAsync)GWT.create(WebUserService.class);
        ((ServiceDefTarget)CShell.usersvc).setServiceEntryPoint("/usersvc");
        CShell.membersvc = (MemberServiceAsync)GWT.create(MemberService.class);
        ((ServiceDefTarget)CShell.membersvc).setServiceEntryPoint("/membersvc");
        CShell.commentsvc = (CommentServiceAsync)GWT.create(CommentService.class);
        ((ServiceDefTarget)CShell.commentsvc).setServiceEntryPoint("/commentsvc");
        CShell.itemsvc = (ItemServiceAsync)GWT.create(ItemService.class);
        ((ServiceDefTarget)CShell.itemsvc).setServiceEntryPoint("/itemsvc");
        CShell.catalogsvc = (CatalogServiceAsync)GWT.create(CatalogService.class);
        ((ServiceDefTarget)CShell.catalogsvc).setServiceEntryPoint("/catalogsvc");

        // load up our translation dictionaries
        CShell.cmsgs = (ShellMessages)GWT.create(ShellMessages.class);
        CShell.imsgs = (ItemMessages)GWT.create(ItemMessages.class);
        CShell.emsgs = (EditemMessages)GWT.create(EditemMessages.class);
        CShell.dmsgs = (DynamicMessages)GWT.create(DynamicMessages.class);
        CShell.smsgs = (ServerMessages)GWT.create(ServerMessages.class);
    }

    /**
     * Makes sure that our credentials are still valid.
     */
    protected void validateSession (String token)
    {
        if (token != null) {
            CShell.usersvc.validateSession(DeploymentConfig.version, token, 1, new AsyncCallback() {
                public void onSuccess (Object result) {
                    if (result == null) {
                        didLogoff();
                    } else {
                        didLogon((SessionData)result);
                    }
                }
                public void onFailure (Throwable t) {
                    didLogoff();
                }
            });
        } else {
            didLogoff();
        }
    }

    /**
     * Called when the flash client has logged on.
     */
    protected void didLogonFromFlash (String displayName, int memberId, String token)
    {
        validateSession(token);
    }

    /**
     * Called when a web page component wants to request a chat channel opened in the Flash
     * client, of the given type and name.
     */
    protected boolean openChannelRequest (int type, String name, int id)
    {
        return openChannelNative(type, name, id);
    }

    protected void createMappings ()
    {
        _creators.put(Page.ACCOUNT, client.account.index.getCreator());
        _creators.put(Page.ADMIN, client.admin.index.getCreator());
        _creators.put(Page.GAMES, client.games.index.getCreator());
        _creators.put(Page.MAIL, client.mail.index.getCreator());
        _creators.put(Page.ME, client.me.index.getCreator());
        _creators.put(Page.PEOPLE, client.people.index.getCreator());
        _creators.put(Page.SHOP, client.shop.index.getCreator());
        _creators.put(Page.STUFF, client.stuff.index.getCreator());
        _creators.put(Page.SWIFTLY, client.swiftly.index.getCreator());
        _creators.put(Page.WHIRLEDS, client.whirleds.index.getCreator());
        _creators.put(Page.WORLD, client.world.index.getCreator());
    }

    /**
     * Configures top-level functions that can be called by Flash.
     */
    protected static native void configureCallbacks (Application app) /*-{
       $wnd.flashDidLogon = function (displayName, memberId, token) {
           app.@client.shell.Application::didLogonFromFlash(Ljava/lang/String;ILjava/lang/String;)(
               displayName, memberId, token);
       };
       $wnd.openChannel = function (type, name, id) {
           app.@client.shell.Application::openChannelRequest(ILjava/lang/String;I)(type, name, id);
       };
       $wnd.onunload = function (event) {
           var client = $doc.getElementById("asclient");
           if (client) {
               client.onUnload();
           }
           return true;
       };
       $wnd.helloWhirled = function () {
            return true;
       };
       $wnd.setWindowTitle = function (title) {
            var xlater = @client.shell.CShell::cmsgs;
            var msg = xlater.@client.shell.ShellMessages::windowTitle(Ljava/lang/String;)(title);
            @com.google.gwt.user.client.Window::setTitle(Ljava/lang/String;)(msg);
       };
       $wnd.displayPage = function (page, args) {
           @client.shell.Application::go(Ljava/lang/String;Ljava/lang/String;)(page, args);
       };
    }-*/;

    /**
     * The native complement to openChannel.
     */
    protected static native boolean openChannelNative (int type, String name, int id) /*-{
        var client = $doc.getElementById("asclient");
        if (client) {
            client.openChannel(type, name, id);
            return true;
        }
        return false;
    }-*/;

    protected Page _page;
    protected HashMap _creators = new HashMap();

    protected StatusPanel _status;

    protected static String _currentToken = "";
}
