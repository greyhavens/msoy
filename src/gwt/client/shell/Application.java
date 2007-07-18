//
// $Id$

package client.shell;

import java.util.HashMap;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;

import com.threerings.msoy.web.client.MemberService;
import com.threerings.msoy.web.client.MemberServiceAsync;
import com.threerings.msoy.web.client.WebUserService;
import com.threerings.msoy.web.client.WebUserServiceAsync;
import com.threerings.msoy.web.data.WebCreds;
import com.threerings.msoy.web.data.WebIdent;

import client.util.MsoyUI;

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
        return createLink(label, "group", ""+groupId);
    }

    /**
     * Returns a {@link Hyperlink} that displays the details of a given member.
     */
    public static Hyperlink memberViewLink (String label, int memberId)
    {
        return createLink(label, "profile", ""+memberId);
    }

    /**
     * Returns a {@link Hyperlink} that navigates to the specified application page with the
     * specified arguments. A page should use this method to pass itself arguments.
     */
    public static Hyperlink createLink (String label, String page, String args)
    {
        return new Hyperlink(label, createLinkToken(page, args)) {
            public void setText (String text) {
                DOM.setInnerText(DOM.getChild(getElement(), 0), text);
            }
        };
    }

    /**
     * Returns HTML that links to the specified page with the specified arguments.
     */
    public static String createLinkHtml (String label, String page, String args)
    {
        return "<a href=\"#" + createLinkToken(page, args) + "\">" + label + "</a>";
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
        RootPanel.get("status").add(_status = new StatusPanel(this));

        // create our standard navigation panel
        RootPanel.get("navigation").add(_navi = new NaviPanel(_status));

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

        String page = (token == null || token.equals("")) ? "world" : token;
        String args = "";
        int semidx = token.indexOf("-");
        if (semidx != -1) {
            page = token.substring(0, semidx);
            args = token.substring(semidx+1);
        }

        if (!displayPopup(page, args)) {
            displayPage(page, args);
        }
    }

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

    protected void displayPage (String ident, String args)
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
                RootPanel.get("content").clear();
                RootPanel.get("content").add(new Label("Unknown page requested '" + ident + "'."));
                return;
            }

            // create the entry point and fire it up
            _page = creator.createPage();
            _page.init();
            _page.onPageLoad();
        }

        // now tell the page about its arguments
        _page.onHistoryChanged(args);
    }

    protected boolean displayPopup (String ident, String args)
    {
        if ("invite".equals(ident)) {
            InvitationDialog.display(_status, args);

        } else if ("optout".equals(ident)) {
            OptOutDialog.display(args);

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

    protected void clearClient (boolean deferred)
    {
        if (deferred) {
            DeferredCommand.add(new Command() {
                public void execute () {
                    clearClient(false);
                }
            });
        } else {
            WorldClient.clearClient(true);
            _page.clearCloseButton();
        }
    }

    protected void setSeparator (int x) 
    {
        clearSeparator();
        Label div = new Label();
        div.setStyleName("SeparatorFromFlash");
        DOM.setAttribute(div.getElement(), "id", "separatorFromFlash");
        DOM.setStyleAttribute(div.getElement(), "left", x + "px");
        RootPanel.get("navigation").add(div);
    }

    protected void clearSeparator ()
    {
        Element div = DOM.getElementById("separatorFromFlash");
        if (div != null) {
            DOM.removeChild(DOM.getParent(div), div);
        }
    }

    /**
     * Called when the Flash client wishes to display a particular Whirled page. The supplied page
     * and args are composed into a history token and inserted into the history which will trigger
     * the loading of the page in question.
     */
    protected void displayPageExternal (String page, String args)
    {
        // note: the following operation will trigger onHistoryChanged
        History.newItem(createLinkToken(page, args));
    }
        
    protected void createMappings ()
    {
        _creators.put("admin", client.admin.index.getCreator());
        _creators.put("catalog", client.catalog.index.getCreator());
        _creators.put("game", client.game.index.getCreator());
        _creators.put("group", client.group.index.getCreator());
        _creators.put("inventory", client.inventory.index.getCreator());
        _creators.put("mail", client.mail.index.getCreator());
        _creators.put("profile", client.profile.index.getCreator());
        _creators.put("swiftly", client.swiftly.index.getCreator());
        _creators.put("world", client.world.index.getCreator());
        _creators.put("wrap", client.wrap.index.getCreator());
    }

    /**
     * Configures top-level functions that can be called by Flash when it wants to tell us about
     * things.
     */
    protected static native void configureCallbacks (Application app) /*-{
       $wnd.flashDidLogon = function (displayName, memberId, token) {
           app.@client.shell.Application::didLogonFromFlash(Ljava/lang/String;ILjava/lang/String;)(displayName, memberId, token);
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
            app.@client.shell.Application::displayPageExternal(Ljava/lang/String;Ljava/lang/String;)(page, args);
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
