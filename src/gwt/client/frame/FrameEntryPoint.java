//
// $Id$

package client.frame;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.data.all.ReferralInfo;
import com.threerings.msoy.web.data.SessionData;

import client.shell.Args;
import client.shell.BrowserTest;
import client.shell.CShell;
import client.shell.FrameHeader;
import client.shell.Page;
import client.shell.Session;
import client.shell.ShellMessages;
import client.shell.TrackingCookie;
import client.shell.WorldClient;
import client.util.Link;

/**
 * Handles the outer shell of the Whirled web application. Loads pages into an iframe and also
 * handles displaying the Flash client.
 */
public class FrameEntryPoint
    implements EntryPoint, HistoryListener, Session.Observer, client.shell.Frame,
               WorldClient.Container
{
    // from interface EntryPoint
    public void onModuleLoad ()
    {
        CShell.frame = this;

        // TODO: listen for resize, resize our client scroller and iframe

        // our main frame never scrolls
        Window.enableScrolling(false);

        // listen for logon/logoff
        Session.addObserver(this);

        // load up various JavaScript dependencies
        for (int ii = 0; ii < JS_DEPENDS.length; ii += 2) {
            Element e = DOM.getElementById(JS_DEPENDS[ii]);
            if (e != null) {
                DOM.setElementAttribute(e, "src", JS_DEPENDS[ii+1]);
            }
        }

        // set up the callbackd that our flash clients can call
        configureCallbacks(this);

        // wire ourselves up to the history-based navigation mechanism
        History.addHistoryListener(this);
        _currentToken = History.getToken();

        // validate our session which will dispatch a didLogon or didLogoff
        Session.validate();

        // create our header, dialog and popup
        _header = new FrameHeader(new ClickListener() {
            public void onClick (Widget sender) {
                if (_closeToken != null) {
                    closeContent();
                } else if (CShell.isGuest()) {
                    History.newItem("");
                } else {
                    Link.go(Page.WORLD, "m" + CShell.getMemberId());
                }
            }
        });
//         _dialog = new Dialog();
//         _popup = new PopupDialog();

        // clear out the loading HTML so we can display a browser warning or load Whirled
        DOM.setInnerHTML(RootPanel.get(LOADING_AND_TESTS).getElement(), "");

        // If the browser is unsupported, hide the page (still being built) and show a warning.
        ClickListener continueClicked = new ClickListener() {
            public void onClick (Widget widget) {
                // close the warning and show the page if the visitor choose to continue
                RootPanel.get(LOADING_AND_TESTS).clear();
                RootPanel.get(LOADING_AND_TESTS).setVisible(false);
                RootPanel.get(SITE_CONTAINER).setVisible(true);
            }
        };
        Widget warningDialog = BrowserTest.getWarningDialog(continueClicked);
        if (warningDialog != null) {
            RootPanel.get(SITE_CONTAINER).setVisible(false);
            RootPanel.get(LOADING_AND_TESTS).add(warningDialog);
        } else {
            RootPanel.get(LOADING_AND_TESTS).clear();
            RootPanel.get(LOADING_AND_TESTS).setVisible(false);
        }

        // TEMP: always show the header
        setHeaderVisible(true);
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

        CShell.log("Displaying page [page=" + page + ", args=" + args + "].");

        // pull the affiliate id out of the URL. it will be of the form: "aid_A_V_C", consisting of
        // three components: the affiliate ID, the entry vector ID, and the creative (ad) ID.
        int aidIdx = args.indexOf("aid");
        int lastIdx = aidIdx + 3;
        if (aidIdx != -1 && args.getArgCount() > lastIdx) {
            String affiliate = args.get(aidIdx + 1, "");
            String vector = args.get(aidIdx + 2, "");
            String creative = args.get(aidIdx + 3, "");

            // remove the "aid" tag and its three values
            token = Args.compose(args.remove(aidIdx, aidIdx + 4));
            args = new Args();
            args.setToken(token);

            // save our tracking info, but don't overwrite old values
            ReferralInfo ref = new ReferralInfo(
                affiliate, vector, creative, ReferralInfo.makeRandomTracker());
            TrackingCookie.save(ref, false);

        } else {
            if (!TrackingCookie.exists()) {
                String tracker = ReferralInfo.makeRandomTracker();
                TrackingCookie.save(ReferralInfo.makeInstance("", "", "", tracker), false);
            }
        }

        // recreate the page token which we'll pass through to the page (or if it's being loaded
        // for the first time, it will request in a moment with a call to getPageToken)
        _pageToken = Args.compose(args.splice(0));

        // replace the page if necessary
        if (_pageId == null || !_pageId.equals(page)) {
            setPage(page);
        } else {
            setPageToken(_pageToken);
        }

//         // convert the page to GA format and report it to Google Analytics
//         _analytics.report(args.toPath(page));
    }

    // from interface Session.Observer
    public void didLogon (SessionData data)
    {
        WorldClient.didLogon(data.creds);

        if (_pageId != null) {
            setPage(_pageId); // reloads the current page
        } else if (!data.justCreated) {
            onHistoryChanged(_currentToken);
        }
    }

    // from interface Session.Observer
    public void didLogoff ()
    {
        // clear out any current page
        _pageId = null;
        // reload the current page
        onHistoryChanged(_currentToken);
        // close the Flash client if it's open
        closeClient();
    }

    // from interface Frame
    public void setTitle (String title)
    {
        Window.setTitle(title == null ? _cmsgs.bareTitle() : _cmsgs.windowTitle(title));
    }

    // from interface Frame
    public void navigateTo (String token)
    {
        if (!token.equals(_currentToken)) {
            History.newItem(token);
        }
    }

    // from interface Frame
    public void navigateReplace (String token)
    {
        History.back();
        History.newItem(token);
    }

    // frome interface Frame
    public void displayWorldClient (String args, String closeToken)
    {
        WorldClient.displayFlash(args, closeToken, this);
    }

    // from interface Frame
    public void closeClient ()
    {
        WorldClient.clientWillClose();
        _closeToken = null;

        RootPanel.get(CLIENT).clear();
        RootPanel.get(CLIENT).setWidth(Math.max(Window.getClientWidth() - CONTENT_WIDTH, 0) + "px");
        RootPanel.get(CONTENT).setWidth(CONTENT_WIDTH + "px");
        RootPanel.get(CONTENT).setVisible(true);
        // TODO: let the page know to clear its close button

        // if we're on a "world" page, go to a landing page
        if (_currentToken.startsWith(Page.WORLD)) {
            // if we were in a game, go to the games page, otherwise go to me
            Link.go(_currentToken.indexOf("game") == -1 ? Page.ME : Page.GAMES, "");
        }
    }

    // from interface Frame
    public void closeContent ()
    {
        // let the Flash client know that it's being unminimized
        WorldClient.setMinimized(false);

        // restore the client to the full glorious browser width
        RootPanel.get(CONTENT).clear();
        RootPanel.get(CONTENT).setWidth("0px");
        RootPanel.get(CONTENT).setVisible(false);
        RootPanel.get(CLIENT).setWidth("100%");

        // restore the client's URL
        if (_closeToken != null) {
            History.newItem(_closeToken);
        }
    }

    // from interface Frame
    public void setHeaderVisible (boolean visible)
    {
        RootPanel.get(HEADER).remove(_header);
        if (visible) {
            RootPanel.get(HEADER).add(_header);
        }
    }

    // from interface Frame
    public void ensureVisible (Widget widget)
    {
        // TEMP: not used, will go away
    }

    // from interface Frame
    public void showDialog (String title, Widget dialog)
    {
        // remove any existing content
        clearDialog();

        // update the dialog content and add it
//         _dialog.update(title, dialog);
//         RootPanel.get(HEADER).add(_dialog); // TODO: animate this sliding down
    }

    // from interface Frame
    public void showPopupDialog (String title, Widget dialog)
    {
//         _popup.setVisible(false);
//         _popup.update(title, dialog);
//         _popup.setVisible(true);
//         _popup.center();
    }

    // from interface Frame
    public void clearDialog ()
    {
//         RootPanel.get(HEADER).remove(_dialog);
    }

    // from interface Frame
    public void showContent (String pageId, Widget pageContent)
    {
        RootPanel.get(CONTENT).clear();

        // clear out any lingering dialog content
        clearDialog();

        if (pageId != null) {
            // select the appropriate header tab
            _header.selectTab(pageId);
        }

        // let the client know it about to be minimized
        WorldClient.setMinimized(true);
        int clientWidth = Math.max(Window.getClientWidth() - CONTENT_WIDTH, 300);
        RootPanel.get(CLIENT).setWidth(clientWidth + "px");

        // stuff the content into the page and size it properly
        RootPanel.get(CONTENT).add(pageContent);
        RootPanel.get(CONTENT).setWidth(CONTENT_WIDTH + "px");
        RootPanel.get(CONTENT).setVisible(true);

        int ccount = RootPanel.get(CLIENT).getWidgetCount();
        if (ccount == 0) {
            RootPanel.get(CLIENT).add(new HTML("&nbsp;"));
        }
    }

    // from interface WorldClient.Container
    public void setShowingClient (String closeToken)
    {
        // note the current history token so that we can restore it if needed
        _closeToken = (closeToken == null) ? _currentToken : closeToken;

        // clear out our content and the expand/close controls
        RootPanel.get(CONTENT).clear();
        RootPanel.get(CONTENT).setWidth("0px");
        RootPanel.get(CONTENT).setVisible(false);

        // have the client take up all the space
        RootPanel.get(CLIENT).setWidth("100%");

        // make sure the header is showing as we always want the header above the client
        setHeaderVisible(true);
        _header.selectTab(null);
    }

    // from interface WorldClient.Container
    public Panel getClientContainer ()
    {
        RootPanel.get(CLIENT).clear();
        if (Window.getClientHeight() < (HEADER_HEIGHT + CLIENT_HEIGHT)) {
            RootPanel.get(CLIENT).add(_cscroller = new ScrollPanel());
            _cscroller.setHeight((Window.getClientHeight() - NAVI_HEIGHT) + "px");
            return _cscroller;
        } else {
            _cscroller = null;
            return RootPanel.get(CLIENT);
        }
    }

    protected void setPage (String pageId)
    {
        _pageId = pageId;
        RootPanel.get("content").clear();
        Frame iframe = new Frame("/" + _pageId + "/" + _pageId + ".html");
        DOM.setElementProperty(iframe.getElement(), "name", "page");
        iframe.setStyleName("pageIFrame");
        showContent(_pageId, iframe);
    }

    /**
     * Handles a variety of methods called by our iframed page.
     */
    protected String frameCall (String callStr, String[] args)
    {
        Calls call = Enum.valueOf(Calls.class, callStr);
        switch (call) {
        case GET_WEB_CREDS:
            return (CShell.creds == null) ? null : CShell.creds.flatten();
        case GET_PAGE_TOKEN:
            return _pageToken;
        case SET_TITLE:
            setTitle(args[0]);
            return null;
        case NAVIGATE_TO:
            navigateTo(args[0]);
            return null;
        case NAVIGATE_REPLACE:
            navigateReplace(args[0]);
            return null;
        case DISPLAY_WORLD_CLIENT:
            displayWorldClient(args[0], args[1]);
            return null;
        case CLOSE_CLIENT:
            closeClient();
            return null;
        case CLOSE_CONTENT:
            closeContent();
            return null;
        }
        return null; // not reached
    }

    protected void deferredCloseClient ()
    {
        DeferredCommand.addCommand(new Command() {
            public void execute () {
                closeClient();
            }
        });
    }

    /**
     * Configures top-level functions that can be called by Flash.
     */
    protected static native void configureCallbacks (FrameEntryPoint entry) /*-{
       $wnd.onunload = function (event) {
           var client = $doc.getElementById("asclient");
           if (client) {
               client.onUnload();
           }
           return true;
       };
       $wnd.frameCall = function (action, args) {
           return entry.@client.frame.FrameEntryPoint::frameCall(Ljava/lang/String;[Ljava/lang/String;)(action, args);
       };
       $wnd.helloWhirled = function () {
            return true;
       };
       $wnd.setWindowTitle = function (title) {
           entry.@client.frame.FrameEntryPoint::setTitle(Ljava/lang/String;)(title);
       };
       $wnd.setGuestId = function (guestId) {
           @client.shell.CShell::setGuestId(I)(guestId);
       };
       $wnd.clearClient = function () {
            entry.@client.frame.FrameEntryPoint::deferredCloseClient()();
       };
       $wnd.getReferral = function () {
           return @client.shell.TrackingCookie::getAsObject()();
       };
       $wnd.setReferral = function (ref) {
           @client.shell.TrackingCookie::saveAsObject(Ljava/lang/Object;Z)(ref, true);
       };
       $wnd.toggleClientHeight = function () {
           @client.util.FlashClients::toggleClientHeight()();
       }
    }-*/;

    /**
     * Passes a page's current token down into our page frame.
     */
    protected static native void setPageToken (String token) /*-{
        try {
            var page = $wnd.frames['page'];
            page.setPageToken(token);
        } catch (e) {
            // oh well, nothing to be done
        }
    }-*/;

    protected String _currentToken = "";
    protected String _pageToken = "";
    protected String _pageId;
    protected String _closeToken;

    protected FrameHeader _header;
    protected ScrollPanel _cscroller;

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);

    // constants for our top-level elements
    protected static final String HEADER = "header";
    protected static final String CONTENT = "content";
    protected static final String CLIENT = "client";
    protected static final String SITE_CONTAINER = "ctable";
    protected static final String LOADING_AND_TESTS = "loadingAndTests";

    /** Enumerates our Javascript dependencies. */
    protected static final String[] JS_DEPENDS = {
        "swfobject", "/js/swfobject.js",
        "md5", "/js/md5.js",
        // TODO: put this on the account registration page
        // "recaptcha", "http://api.recaptcha.net/js/recaptcha_ajax.js",
        "googanal", "http://www.google-analytics.com/ga.js",
    };
}
