//
// $Id$

package client.shell;

import java.util.HashMap;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.web.client.CatalogService;
import com.threerings.msoy.web.client.CatalogServiceAsync;
import com.threerings.msoy.web.client.CommentService;
import com.threerings.msoy.web.client.CommentServiceAsync;
import com.threerings.msoy.web.client.ItemService;
import com.threerings.msoy.web.client.ItemServiceAsync;
import com.threerings.msoy.web.client.MemberService;
import com.threerings.msoy.web.client.MemberServiceAsync;
import com.threerings.msoy.web.client.WebUserService;
import com.threerings.msoy.web.client.WebUserServiceAsync;
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

    /**
     * Returns a {@link Hyperlink} that displays the details of a given group.
     */
    public static Hyperlink groupViewLink (String label, int groupId)
    {
        return createLink(label, Page.GROUP, ""+groupId);
    }

    /**
     * Returns a {@link Hyperlink} that displays the details of a given member.
     */
    public static Hyperlink memberViewLink (String label, int memberId)
    {
        return createLink(label, Page.PROFILE, ""+memberId);
    }

    /**
     * Returns a {@link Hyperlink} that displays the details of a given member.
     */
    public static Hyperlink memberViewLink (MemberName name)
    {
        return createLink(name.toString(), Page.PROFILE, ""+name.getMemberId());
    }

    /**
     * Returns a {@link Hyperlink} that navigates to the specified application page with the
     * specified arguments. A page should use this method to pass itself arguments.
     */
    public static Hyperlink createLink (String label, String page, String args)
    {
        Hyperlink link = new Hyperlink(label, createLinkToken(page, args)) {
            public void setText (String text) {
                DOM.setInnerText(DOM.getChild(getElement(), 0), text);
            }
        };
        link.addStyleName("inline");
        return link;
    }

    /**
     * Returns HTML that links to the specified page with the specified arguments.
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
            CShell.app._page.setContentMinimized(false, null);
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

        // create our status/logon panel
        _status = new StatusPanel(this);
        RootPanel status = RootPanel.get("status");
        if (status != null) {
            status.add(_status);
        }

        // add the logo, with link to My Whirled/Whirledwide
        Image logo = new Image("/images/header/header_logo.png");
        logo.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                boolean loggedIn = CShell.creds != null;
                go(Page.WHIRLED, loggedIn ? "mywhirled" : "whirledwide");
            }
        });
        RootPanel logoPanel = RootPanel.get("logo");
        if (logoPanel != null) {
            logoPanel.add(logo);
        }

        // create our standard navigation panel
        _navi = new NaviPanel(_status);
        RootPanel navi = RootPanel.get(Page.NAVIGATION);
        if (navi != null) {
            navi.add(_navi);
        }

        // wire ourselves up to the history-based navigation mechanism
        History.addHistoryListener(this);
        _currentToken = History.getToken();

        // initialize the status panel
        _status.init();

        // now wait for our status panel to call didLogon() or didLogoff()
    }

    // from interface HistoryListener
    public void onHistoryChanged (String token)
    {
        _currentToken = token;

        String page = (token == null || token.equals("")) ? Page.WHIRLED : token;
        Args args = new Args();
        int dashidx = token.indexOf("-");
        if (dashidx != -1) {
            page = token.substring(0, dashidx);
            args.setToken(token.substring(dashidx+1));
        }

        if (!displayPopup(page, args)) {
            displayPage(page, args);
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

    protected void displayPage (String ident, final Args args)
    {
        // replace the page if necessary
        if (_page == null || !_page.getPageId().equals(ident)) {
            // tell any existing page that it's being unloaded
            if (_page != null) {
                _page.onPageUnload();
                _page = null;
            }

            // locate the creator for this page
            Page.Creator creator = (Page.Creator)_creators.get(ident);
            if (creator == null) {
                RootPanel.get(Page.CONTENT).clear();
                RootPanel.get(Page.CONTENT).add(
                    new Label("Unknown page requested '" + ident + "'."));
                return;
            }

            // create the entry point and fire it up
            _page = creator.createPage();
            _page.init();
            _page.onPageLoad();

            // tell the page about its arguments
            _page.onHistoryChanged(args);

        } else {
            _page.setContentMinimized(false, new Command() {
                public void execute () {
                    // now tell the page about its arguments
                    _page.onHistoryChanged(args);
                }
            });
        }
    }

    protected boolean displayPopup (String ident, Args args)
    {
        if ("invite".equals(ident)) {
            InvitationDialog.display(_status, args.get(0, ""));

        } else if ("optout".equals(ident)) {
            OptOutDialog.display(args.get(0, ""));

        } else if ("resetpw".equals(ident)) {
            ResetPasswordDialog.display(args);

        } else {
            return false;
        }

        return true;
    }

    /**
     * Called when the player logs on (or when our session is validated).
     */
    protected void didLogon (WebCreds creds)
    {
        CShell.creds = creds;
        CShell.ident = new WebIdent(creds.getMemberId(), creds.token);
        _navi.didLogon(creds);
        WorldClient.didLogon(creds);

        if (_page == null) {
            // we can now load our starting page
            onHistoryChanged(_currentToken);
        } else {
            _page.didLogon(creds);
        }
    }

    /**
     * Called when the player logs off.
     */
    protected void didLogoff ()
    {
        CShell.creds = null;
        CShell.ident = null;
        _navi.didLogoff();
        WorldClient.didLogoff();

        if (_page == null) {
            // we can now load our starting page
            onHistoryChanged(_currentToken);
        } else {
            _page.didLogoff();
        }
    }

    /**
     * Called when the flash client has logged on.
     */
    protected void didLogonFromFlash (String displayName, int memberId, String token)
    {
        _status.validateSession(token);
    }

    /**
     * Called when a web page component wants to request a chat channel opened in the Flash
     * client, of the given type and name.
     */
    protected boolean openChannelRequest (int type, String name, int id)
    {
        return openChannelNative(type, name, id);
    }

    protected void restoreClient ()
    {
        _page.setContentMinimized(true, null);
    }

    protected void clearClient (boolean deferred)
    {
        _page.clearClient(deferred);
    }

    protected void setSeparator (int x)
    {
        clearSeparator();
        Label div = new Label();
        div.setStyleName("SeparatorFromFlash");
        DOM.setAttribute(div.getElement(), "id", "separatorFromFlash");
        DOM.setStyleAttribute(div.getElement(), "left", x + "px");
        RootPanel.get(Page.NAVIGATION).add(div);
    }

    protected void clearSeparator ()
    {
        Element div = DOM.getElementById("separatorFromFlash");
        if (div != null) {
            DOM.removeChild(DOM.getParent(div), div);
        }
    }

    protected void createMappings ()
    {
        _creators.put(Page.ADMIN, client.admin.index.getCreator());
        _creators.put(Page.CATALOG, client.catalog.index.getCreator());
        _creators.put(Page.GAME, client.game.index.getCreator());
        _creators.put(Page.GROUP, client.group.index.getCreator());
        _creators.put(Page.INVENTORY, client.inventory.index.getCreator());
        _creators.put(Page.MAIL, client.mail.index.getCreator());
        _creators.put(Page.PROFILE, client.profile.index.getCreator());
        _creators.put(Page.SWIFTLY, client.swiftly.index.getCreator());
        _creators.put(Page.WHIRLED, client.whirled.index.getCreator());
        _creators.put(Page.WORLD, client.world.index.getCreator());
        _creators.put(Page.WRAP, client.wrap.index.getCreator());
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
       }
       $wnd.restoreClient = function () {
            app.@client.shell.Application::restoreClient()();
       }
       $wnd.clearClient = function () {
            app.@client.shell.Application::clearClient(Z)(true);
       }
       $wnd.setSeparator = function (x) {
            app.@client.shell.Application::setSeparator(I)(x);
       }
       $wnd.clearSeparator = function () {
            app.@client.shell.Application::clearSeparator()();
       }
       $wnd.setWindowTitle = function (title) {
            var xlater = @client.shell.CShell::cmsgs;
            var msg = xlater.@client.shell.ShellMessages::windowTitle(Ljava/lang/String;)(title);
            @com.google.gwt.user.client.Window::setTitle(Ljava/lang/String;)(msg);
       }
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

    protected NaviPanel _navi;
    protected StatusPanel _status;

    protected static String _currentToken = "";
}
