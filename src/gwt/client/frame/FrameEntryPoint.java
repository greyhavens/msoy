//
// $Id$

package client.frame;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.WindowResizeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.data.all.ReferralInfo;
import com.threerings.msoy.web.client.MemberService;
import com.threerings.msoy.web.client.MemberServiceAsync;
import com.threerings.msoy.web.data.Invitation;
import com.threerings.msoy.web.data.SessionData;

import client.shell.Args;
import client.shell.BrowserTest;
import client.shell.CShell;
import client.shell.FrameHeader;
import client.shell.Pages;
import client.shell.Session;
import client.shell.ShellMessages;
import client.shell.TitleBar;
import client.shell.TrackingCookie;
import client.shell.WorldClient;
import client.util.ArrayUtil;
import client.util.FlashClients;
import client.util.FlashVersion;
import client.util.Link;
import client.util.MsoyCallback;
import client.util.ServiceUtil;
import client.util.events.FlashEvent;
import client.util.events.FlashEvents;

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
        // set up our CShell singleton
        CShell.init(this);

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
                    Link.go(Pages.WORLD, "m" + CShell.getMemberId());
                }
            }
        });
        _header.setVisible(false);
        RootPanel.get(PAGE).add(_header);

//         _dialog = new Dialog();
//         _popup = new PopupDialog();

        // clear out the loading HTML so we can display a browser warning or load Whirled
        DOM.setInnerHTML(RootPanel.get(LOADING).getElement(), "");

        // If the browser is unsupported, hide the page (still being built) and show a warning.
        ClickListener continueClicked = new ClickListener() {
            public void onClick (Widget widget) {
                // close the warning and show the page if the visitor choose to continue
                RootPanel.get(LOADING).clear();
                RootPanel.get(LOADING).setVisible(false);
            }
        };
        Widget warningDialog = BrowserTest.getWarningDialog(continueClicked);
        if (warningDialog != null) {
            RootPanel.get(LOADING).add(warningDialog);
        } else {
            RootPanel.get(LOADING).clear();
            RootPanel.get(LOADING).setVisible(false);
        }
    }

    // from interface HistoryListener
    public void onHistoryChanged (String token)
    {
        _currentToken = token;

        Pages page;
        try {
            page = Enum.valueOf(Pages.class, token.split("-")[0].toUpperCase());
        } catch (Exception e) {
            page = getLandingPage();
        }
        Args args = new Args();
        int dashidx = token.indexOf("-");
        if (dashidx != -1) {
            args.setToken(token.substring(dashidx+1));
        }

        CShell.log("Displaying page [page=" + page + ", args=" + args + "].");

        // do some special processing if this is an invitation link
        if (page == Pages.ME && args.get(0, "").equals("i") && CShell.isGuest()) {
            // load up the invitation information and save it
            String inviteId = args.get(1, "");
            if (_activeInvite == null || !_activeInvite.inviteId.equals(inviteId)) {
                _membersvc.getInvitation(inviteId, true, new MsoyCallback<Invitation>() {
                    public void onSuccess (Invitation invite) {
                        _activeInvite = invite;
                        // also configure our tracking cookie
                        TrackingCookie.save(new ReferralInfo(
                                                ""+invite.inviter.getMemberId(), EMAIL_VECTOR,
                                                "", ReferralInfo.makeRandomTracker()), false);
                    }
                });
            }
            // and send them to the landing page
            Link.go(Pages.LANDING, "");
            return;
        }

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
        if (_page != page) {
            setPage(page);
        } else {
            setPageToken(_pageToken, _iframe.getElement());
        }

//         // convert the page to GA format and report it to Google Analytics
//         _analytics.report(args.toPath(page));
    }

    // from interface Session.Observer
    public void didLogon (SessionData data)
    {
        WorldClient.didLogon(data.creds);

        if (_page == Pages.LANDING) {
            Link.go(Pages.ME, "");
        } else if (_page != null) {
            setPage(_page); // reloads the current page
        } else if (!data.justCreated) {
            onHistoryChanged(_currentToken);
        }
    }

    // from interface Session.Observer
    public void didLogoff ()
    {
        // clear out any current page
        _page = null;
        // reload the current page
        onHistoryChanged(_currentToken);
        // close the Flash client if it's open
        closeClient();
    }

    // from interface Frame
    public void setTitle (String title)
    {
        Window.setTitle(title == null ? _cmsgs.bareTitle() : _cmsgs.windowTitle(title));
        if (title != null && _bar != null) {
            _bar.setTitle(title);
        }
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

    // from interface Frame
    public void displayWorldClient (String args, String closeToken)
    {
        WorldClient.displayFlash(args, closeToken, this);
    }

    // from interface Frame
    public void closeClient ()
    {
        WorldClient.clientWillClose();
        _closeToken = null;

        if (_client != null) {
            RootPanel.get(PAGE).remove(_client);
            _client = null;
            if (_content != null) {
                _content.setWidth(CONTENT_WIDTH + "px");
                _content.setVisible(true);
            }
        }

        if (_bar != null) {
            _bar.setCloseVisible(false);
        }

        // if we're on a "world" page, go to a landing page
        if (_currentToken != null && _currentToken.startsWith(Pages.WORLD.getPath())) {
            // if we were in a game, go to the games page, otherwise go to me
            Link.go(_currentToken.indexOf("game") == -1 ? Pages.ME : Pages.GAMES, "");
        }
    }

    // from interface Frame
    public void closeContent ()
    {
        // let the Flash client know that it's being unminimized
        WorldClient.setMinimized(false);

        // clear out the content
        clearContent();

        // restore the client to the full glorious browser width
        if (_client != null) {
            RootPanel.get(PAGE).setWidgetPosition(_client, 0, NAVI_HEIGHT);
            _client.setWidth("100%");
        }

        // restore the client's URL
        if (_closeToken != null) {
            History.newItem(_closeToken);
        }
    }

    // from interface Frame
    public void setHeaderVisible (boolean visible)
    {
        _header.setVisible(visible);
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
    public void showContent (Pages page, Widget pageContent)
    {
        // clear out any old content
        clearContent();

        // clear out any lingering dialog content
        clearDialog();

        // show the header for everything except the landing pages
        setHeaderVisible(Pages.LANDING != page);

        // select the appropriate header tab
        _header.selectTab(page.getTab());

        int contentTop;
        String contentWidth, contentHeight;
        switch (page) {
        case LANDING:
            closeClient(); // no client on the landing page
            // content takes up whole page
            contentWidth = "100%";
            contentHeight = "100%";
            contentTop = 0;

            // the content is just the supplied widget, no extra bits
            _content = pageContent;

            // we won't listen for resize because the iframe is height 100%
            setWindowResizerEnabled(false);
            break;

//         case WORLD:
//             // TODO: handle world page stuff directly, extract rooms page to ROOMS
//             break;

        default:
            // let the client know it about to be minimized
            WorldClient.setMinimized(true);
            int clientWidth = Math.max(Window.getClientWidth() - CONTENT_WIDTH, 300);
            if (_client != null) {
                _client.setWidth(clientWidth + "px");
                RootPanel.get(PAGE).setWidgetPosition(_client, CONTENT_WIDTH, NAVI_HEIGHT);
            }

            // position the content normally
            contentWidth = CONTENT_WIDTH + "px";
            contentHeight = (Window.getClientHeight() - NAVI_HEIGHT) + "px";
            contentTop = NAVI_HEIGHT;

            // add a titlebar to the top of the content
            FlowPanel content = new FlowPanel();
            if (page.getTab() != null) {
                content.add(_bar = TitleBar.create(page.getTab(), new ClickListener() {
                    public void onClick (Widget sender) {
                        closeContent();
                    }
                }));
            }
            pageContent.setWidth(contentWidth);
            pageContent.setHeight((Window.getClientHeight() - HEADER_HEIGHT) + "px");
            content.add(pageContent);
            _content = content;

            // listen for window resize so that we can adjust the size of the content
            setWindowResizerEnabled(true);
            break;
        }

        // size, add and position the content
        _content.setWidth(contentWidth);
        _content.setHeight(contentHeight);
        RootPanel.get(PAGE).add(_content);
        RootPanel.get(PAGE).setWidgetPosition(_content, 0, contentTop);

        // activate our close button if we have a client
        if (_bar != null) {
            _bar.setCloseVisible(FlashClients.clientExists());
        }
    }

    // from interface Frame
    public void dispatchEvent (FlashEvent event)
    {
        // dispatch the event locally
        FlashEvents.internalDispatchEvent(event);

        // forward the event to our subpage
        if (_iframe != null) {
            JavaScriptObject args = JavaScriptObject.createArray();
            event.toJSObject(args);
            forwardEvent(_iframe.getElement(), event.getEventName(), args);
        }
    }

    // from interface Frame
    public void dispatchDidLogon (SessionData data)
    {
        Session.didLogon(data);
    }

    // from interface Frame
    public String md5hex (String text)
    {
        return nmd5hex(text);
    }

    // from interface Frame
    public String checkFlashVersion (int width, int height)
    {
    	return FlashVersion.checkFlashVersion(width, height);
    }

    // from interface Frame
    public Invitation getActiveInvitation ()
    {
        return _activeInvite;
    }

    // from interface WorldClient.Container
    public void setShowingClient (String closeToken)
    {
        // note the current history token so that we can restore it if needed
        _closeToken = (closeToken == null) ? _currentToken : closeToken;

        // hide our content
        if (_content != null) {
            _content.setVisible(false);
        }

        // have the client take up all the space
        if (_client != null) {
            RootPanel.get(PAGE).setWidgetPosition(_client, 0, NAVI_HEIGHT);
            _client.setWidth("100%");
        }

        // make sure the header is showing as we always want the header above the client
        setHeaderVisible(true);
        _header.selectTab(null);
    }

    // from interface WorldClient.Container
    public Panel getClientContainer ()
    {
        if (_client != null) {
            RootPanel.get(PAGE).remove(_client);
            _client = null;
        }

        if (Window.getClientHeight() < (NAVI_HEIGHT + CLIENT_HEIGHT)) {
            _client = new ScrollPanel();
            _client.setHeight((Window.getClientHeight() - NAVI_HEIGHT) + "px");
        } else {
            _client = new SimplePanel();
        }
        _client.setWidth("100%");
        RootPanel.get(PAGE).add(_client);
        RootPanel.get(PAGE).setWidgetPosition(_client, 0, NAVI_HEIGHT);

        return _client;
    }

    protected void setPage (Pages page)
    {
        _page = page;
        _iframe = new Frame("/gwt/" + _page.getPath() + "/" + _page.getPath() + ".html");
        _iframe.setStyleName("pageIFrame");
        showContent(_page, _iframe);
    }

    protected void clearContent ()
    {
        if (_content != null) {
            _bar = null;
            RootPanel.get(PAGE).remove(_content);
            _content = null;
        }
    }

    /**
     * Handles a variety of methods called by our iframed page.
     */
    protected String[] frameCall (String callStr, String[] args)
    {
        Calls call = Enum.valueOf(Calls.class, callStr);
        switch (call) {
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
        case DID_LOGON:
            Session.didLogon(SessionData.unflatten(ArrayUtil.toIterator(args)));
            return null;
        case GET_WEB_CREDS:
            return (CShell.creds == null) ? null : CShell.creds.flatten().toArray(new String[0]);
        case GET_PAGE_TOKEN:
            return new String[] { _pageToken };
        case GET_MD5:
            return new String[] { nmd5hex(args[0]) };
        case CHECK_FLASH_VERSION:
            return new String[] {
                checkFlashVersion(Integer.valueOf(args[0]), Integer.valueOf(args[1]))
            };
        case GET_ACTIVE_INVITE:
            return _activeInvite == null ? null : _activeInvite.flatten().toArray(new String[0]);
        }
        CShell.log("Got unknown frameCall request [call=" + call + "].");
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

    protected void setWindowResizerEnabled (boolean enabled)
    {
        if (!enabled && _resizer != null) {
            Window.removeWindowResizeListener(_resizer);
            _resizer = null;

        } else if (enabled && _resizer == null) {
            _resizer = new WindowResizeListener() {
                public void onWindowResized (int width, int height) {
                    if (_content != null) {
                        _content.setHeight((Window.getClientHeight() - NAVI_HEIGHT) + "px");
                    }
                    if (_iframe != null) {
                        _iframe.setHeight((Window.getClientHeight() - HEADER_HEIGHT) + "px");
                    }
                }
            };
            Window.addWindowResizeListener(_resizer);
        }
    }

    protected Pages getLandingPage ()
    {
        return CShell.isGuest() ? Pages.LANDING : Pages.ME;
    }

    /**
     * Called when Flash or our inner Page frame wants us to dispatch an event.
     */
    protected void triggerEvent (String eventName, JavaScriptObject args)
    {
        FlashEvent event = FlashEvents.createEvent(eventName, args);
        if (event != null) {
            dispatchEvent(event);
        }
    }

    /**
     * Called when Flash wants us to display a page.
     */
    protected static void displayPage (String page, String args)
    {
    	try {
            Link.go(Enum.valueOf(Pages.class, page.toUpperCase()), args);
    	} catch (Exception e) {
            CShell.log("Unable to display page from Flash [page=" + page + 
                       ", args=" + args + "].", e);
    	}
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
       $wnd.displayPage = function (page, args) {
           @client.frame.FrameEntryPoint::displayPage(Ljava/lang/String;Ljava/lang/String;)(page, args);
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
       $wnd.triggerFlashEvent = function (eventName, args) {
           entry.@client.frame.FrameEntryPoint::triggerEvent(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(eventName, args);
       }
    }-*/;

    /**
     * Passes a page's current token down into our page frame.
     */
    protected static native void setPageToken (String token, Element frame) /*-{
        try {
            if (frame.contentWindow.setPageToken) {
                frame.contentWindow.setPageToken(token);
            }
        } catch (e) {
            if ($wnd.console) {
                $wnd.console.log("Failed to set page token [token=" + token + ", error=" + e + "].");
            }
        }
    }-*/;

    /**
     * Forwards a Flash event to the page frame.
     */
    protected static native void forwardEvent (
        Element frame, String name, JavaScriptObject args) /*-{
        try {
            if (frame.contentWindow.triggerEvent) {
                frame.contentWindow.triggerEvent(name, args);
            }
        } catch (e) {
            if ($wnd.console) {
                $wnd.console.log("Failed to forward event [name=" + name + ", error=" + e + "].");
            }
        }
    }-*/;

    /** MD5 hashes the supplied text and returns the hex encoded hash value. */
    public native static String nmd5hex (String text) /*-{
        return $wnd.hex_md5(text);
    }-*/;

    protected String _currentToken = "";
    protected String _pageToken = "";
    protected Pages _page;
    protected String _closeToken;

    protected FrameHeader _header;
    protected Widget _content;
    protected TitleBar _bar;
    protected Frame _iframe;
    protected Panel _client;

    /** If the user arrived via an invitation, we'll store that here during their session. */
    protected Invitation _activeInvite;

    /** Handles window resizes. */
    protected WindowResizeListener _resizer;

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final MemberServiceAsync _membersvc = (MemberServiceAsync)
        ServiceUtil.bind(GWT.create(MemberService.class), MemberService.ENTRY_POINT);

    // constants for our top-level elements
    protected static final String PAGE = "page";
    protected static final String LOADING = "loading";

    /** This vector string represents an email invite */
    protected static final String EMAIL_VECTOR = "emailInvite";

    /** Enumerates our Javascript dependencies. */
    protected static final String[] JS_DEPENDS = {
        "swfobject", "/js/swfobject.js",
        "md5", "/js/md5.js",
        "googanal", "http://www.google-analytics.com/ga.js",
    };
}
