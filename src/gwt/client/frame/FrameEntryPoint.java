//
// $Id$

package client.frame;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
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
    implements EntryPoint, HistoryListener, Session.Observer, client.shell.Frame
{
    // from interface EntryPoint
    public void onModuleLoad ()
    {
        CShell.frame = this;

        // our main frame never scrolls
        Window.enableScrolling(false);

        // listen for logon/logoff
        Session.addObserver(this);

        // set up the callbackd that our flash clients can call
        configureCallbacks(this, CShell.frame);

        // wire ourselves up to the history-based navigation mechanism
        History.addHistoryListener(this);
        _currentToken = History.getToken();

        // validate our session which will dispatch a didLogon or didLogoff
        Session.validate();

        // create our header, dialog and popup
        _header = new FrameHeader(new ClickListener() {
            public void onClick (Widget sender) {
                if (closeContent()) {
                    // peachy, nothing else to do
                } else {
                    if (CShell.isGuest()) {
                        History.newItem("");
                    } else {
                        Link.go(Page.WORLD, "m" + CShell.getMemberId());
                    }
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
            _pageId = page;
            RootPanel.get("content").clear();
            Frame iframe = new Frame("/" + _pageId + "/" + _pageId + ".html");
            iframe.setStyleName("pageIFrame");
            showContent(_pageId, iframe);
        } else {
            // TODO: pass our arguments through to our iframed page
        }

//         // convert the page to GA format and report it to Google Analytics
//         _analytics.report(args.toPath(page));
    }

    // from interface Session.Observer
    public void didLogon (SessionData data)
    {
        // WorldClient.didLogon(data.creds);

        if (_pageId != null) {
            // TODO: tell our page that we just logged on
        } else if (_currentToken != null && !data.justCreated) {
            onHistoryChanged(_currentToken);
        }

        // TEMP: show a header
        CShell.frame.setHeaderVisible(true);
    }

    // from interface Session.Observer
    public void didLogoff ()
    {
        if (_pageId == null) {
            // we can now load our starting page
            onHistoryChanged(_currentToken);
        } else {
            CShell.frame.closeClient(false);
            // TODO: tell our page that we just logged off
        }

        // TEMP: show a header
        CShell.frame.setHeaderVisible(true);
    }

    // from interface Frame
    public void setTitle (String title)
    {
        Window.setTitle(title == null ? _cmsgs.bareTitle() : _cmsgs.windowTitle(title));
    }

    // from interface Frame
    public void setShowingClient (String closeToken)
    {
        // note the current history token so that we can restore it if needed
        _closeToken = closeToken;

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

    // from interface Frame
    public void closeClient (boolean deferred)
    {
        if (deferred) {
            DeferredCommand.addCommand(new Command() {
                public void execute () {
                    closeClient(false);
                }
            });
            return;
        }

        WorldClient.clientWillClose();
        _closeToken = null;
        RootPanel.get(CLIENT).clear();
        RootPanel.get(CLIENT).setWidth(Math.max(Window.getClientWidth() - CONTENT_WIDTH, 0) + "px");
        RootPanel.get(CONTENT).setWidth(CONTENT_WIDTH + "px");
        RootPanel.get(CONTENT).setVisible(true);
        // TODO: let the page know to clear its close button

        // if we're on a "world" page, go to a landing page
        String curToken = History.getToken();
        if (curToken.startsWith(Page.WORLD)) {
            // if we were in a game, go to the games page, otherwise go to me
            Link.go(curToken.indexOf("game") == -1 ? Page.ME : Page.GAMES, "");
        }
    }

    // from interface Frame
    public boolean closeContent ()
    {
        if (_closeToken == null) {
            return false;
        }

        // let the Flash client know that it's being unminimized
        WorldClient.setMinimized(false);

        // restore the client to the full glorious browser width
        RootPanel.get(CONTENT).clear();
        RootPanel.get(CONTENT).setWidth("0px");
        RootPanel.get(CONTENT).setVisible(false);
        RootPanel.get(CLIENT).setWidth("100%");

        // restore the client's URL
        History.newItem(_closeToken);

        return true;
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

    protected String getPageToken ()
    {
        return _pageToken;
    }

    /**
     * Configures top-level functions that can be called by Flash.
     */
    protected static native void configureCallbacks (FrameEntryPoint entry, client.shell.Frame frame) /*-{
       $wnd.onunload = function (event) {
           var client = $doc.getElementById("asclient");
           if (client) {
               client.onUnload();
           }
           return true;
       };
       $wnd.getPageToken = function () {
           return entry.@client.frame.FrameEntryPoint::getPageToken()();
       };
       $wnd.helloWhirled = function () {
            return true;
       };
       $wnd.setWindowTitle = function (title) {
            frame.@client.shell.Frame::setTitle(Ljava/lang/String;)(title);
       };
       $wnd.displayPage = function (page, args) {
           @client.util.Link::go(Ljava/lang/String;Ljava/lang/String;)(page, args);
       };
       $wnd.setGuestId = function (guestId) {
           @client.shell.CShell::setGuestId(I)(guestId);
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
}
