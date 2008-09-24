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
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.ReferralInfo;
import com.threerings.msoy.data.all.VisitorInfo;
import com.threerings.msoy.web.client.WebMemberService;
import com.threerings.msoy.web.client.WebMemberServiceAsync;
import com.threerings.msoy.web.client.WebUserService;
import com.threerings.msoy.web.client.WebUserServiceAsync;
import com.threerings.msoy.web.data.Invitation;
import com.threerings.msoy.web.data.LaunchConfig;
import com.threerings.msoy.web.data.SessionData;

import client.shell.Args;
import client.shell.BrowserTest;
import client.shell.CShell;
import client.shell.HttpReferrerCookie;
import client.shell.Pages;
import client.shell.Session;
import client.shell.ShellMessages;
import client.shell.TrackingCookie;
import client.shell.VisitorCookie;
import client.ui.BorderedDialog;
import client.util.ArrayUtil;
import client.util.FlashClients;
import client.util.FlashVersion;
import client.util.Link;
import client.util.MsoyCallback;
import client.util.ServiceUtil;
import client.util.events.FlashEvent;
import client.util.events.FlashEvents;
import client.util.events.GotGuestIdEvent;

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

        // initialize our GA handler
        _analytics.init();

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

        String pagename = "";
        Pages page;
        try {
            pagename = token.split("-")[0];
            page = Enum.valueOf(Pages.class, pagename.toUpperCase());
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
                        maybeCreateReferral(""+invite.inviter.getMemberId(), EMAIL_VECTOR, "");
                    }
                });
            }
            // and send them to the landing page
            Link.go(Pages.LANDING, "");
            return;
        }

        String vector = null;
        VisitorInfo info = (CShell.visitor != null) ? CShell.visitor : VisitorCookie.get();

        // pull out the vector and visitor id from the URL.
        // they will be of the form: "vec_VECTOR" and "vis_VISITORID" respectively.
        ExtractedParam afterVector = extractParams("vec", pagename, token, args);
        if (afterVector != null) {
            vector = afterVector.value;
            token = afterVector.newToken;
            args = afterVector.newArgs;
        }

        ExtractedParam afterVisitor = extractParams("vid", pagename, token, args);
        if (afterVisitor != null) {
            if (info == null || !info.isAuthoritative) { // only override client-side info
                info = new VisitorInfo(afterVisitor.value, false);
            }
            token = afterVisitor.newToken;
            args = afterVisitor.newArgs;
        }

        // START LEGACY CODE - to be removed after all of our ads and embeds are transitioned
        //
        // pull the affiliate id out of the URL. it will be of the form: "aid_A_V_C", consisting of
        // three components: the affiliate ID, the entry vector ID, and the creative (ad) ID.
        int aidIdx = args.indexOf("aid");
        int lastIdx = aidIdx + 3;
        if (aidIdx != -1 && args.getArgCount() > lastIdx) {
            String aff = args.get(aidIdx + 1, "");
            String vec = args.get(aidIdx + 2, "");
            String cre = args.get(aidIdx + 3, "");

            // remove the "aid" tag and its three values
            token = Args.compose((Object[])args.remove(aidIdx, aidIdx + 4));
            args = new Args();
            args.setToken(token);

            // save our tracking info, but don't overwrite old values
            maybeCreateReferral(aff, vec, cre);

            // save our info
            vector = aff + ":" + vec + ":" + cre;
        }
        // END LEGACY CODE

        // if we got a new vector from the URL, record it in Panopticon
        if (vector != null) {
            final VisitorInfo constInfo = info;
            _membersvc.trackVectorAssociation(info, vector, new AsyncCallback<Void>() {
                public void onSuccess (Void result) {
                    CShell.log("Saved vector association for " + constInfo);
                    if (! constInfo.isAuthoritative) {
                        VisitorCookie.save(constInfo, true);
                    }
                }
                public void onFailure (Throwable caught) {
                    CShell.log("Failed to send vector creation to server.", caught);
                }
            });
        }

        // if we still don't have a tracking cookie, try to manufacture one from the HTTP Referer
        // header, which the server should have saved for us.
        if (!TrackingCookie.exists()) {
            if (HttpReferrerCookie.exists()) {
                String ref = HttpReferrerCookie.get();
                maybeCreateReferral(ref, token, "");
            } else {
                maybeCreateReferral("", "", "");
            }
        }

        // recreate the page token which we'll pass through to the page (or if it's being loaded
        // for the first time, it will request in a moment with a call to getPageToken)
        _pageToken = Args.compose((Object[])args.splice(0));

        // replace the page if necessary
        if (_page != page || _page == Pages.WORLD) {
            setPage(page);
        } else {
            // reset our navigation as we're not changing pages but need to give the current page a
            // fresh subnavigation palette
            if (_bar != null) {
                _bar.resetNav();
            }
            setPageToken(_pageToken, _iframe.getElement());
        }

        // convert the page to GA format and report it to Google Analytics
        _analytics.report(args.toPath(page));
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
    public void addNavLink (String label, Pages page, String args)
    {
        _bar.addContextLink(label, page, args);
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
        // mysteriously, if we go back() and then newItem() our current location, nothing happens
        // at all, no history changed event, no browser navigation, nothing; I think this might
        // have to do with some of the weird-ass timer based hackery that GWT has to do to make the
        // whole browser history thing work at all
        if (token.equals(_currentToken)) {
            onHistoryChanged(_currentToken);
        } else {
            History.back();
            History.newItem(token);
        }
    }

    // from interface Frame
    public void closeClient ()
    {
        WorldClient.clientWillClose();
        _closeToken = null;

        if (_bar != null) {
            _bar.setCloseVisible(false);
        }

        if (_client != null) {
            RootPanel.get(PAGE).remove(_client);
            _client = null;
            if (_content != null) {
                _content.setWidth(CONTENT_WIDTH + "px");
                _content.setVisible(true);
            }

            // if we're on a "world" page, go to a landing page
            if (_currentToken != null && _currentToken.startsWith(Pages.WORLD.getPath())) {
                // if we were in a game, go to the games page, otherwise go to the landing
                Link.go(_currentToken.indexOf("game") == -1 ? getLandingPage() : Pages.GAMES, "");
            }
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
    public void showDialog (String title, Widget dialog)
    {
        // remove any existing content
        clearDialog();

        _dialog = new BorderedDialog(false, false, false) {
            protected void onClosed (boolean autoClosed) {
                _dialog = null;
            }
        };
        _dialog.setHeaderTitle(title);
        _dialog.setContents(dialog);
        _dialog.show();
    }

    // from interface Frame
    public void clearDialog ()
    {
        if (_dialog != null) {
            _dialog.hide();
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

    public static class ExtractedParam
    {
        public String value;
        public String newToken;
        public Args newArgs;
    }

    protected ExtractedParam extractParams (String key, String pagename, String token, Args args)
    {
        int idx = args.indexOf(key);

        if (idx == -1 || args.getArgCount() < idx + 2) {
            return null; // we have no key, or no value
        }

        ExtractedParam result = new ExtractedParam();

        // get the result
        result.value = args.get(idx + 1, null);
        // remove the key tag and its value from the URL
        String shortened = Args.compose((Object[])args.remove(idx, idx + 2));
        result.newArgs = Args.fromToken(shortened);
        result.newToken = pagename + "-" + shortened;
        return result;
    }

    protected void setPage (Pages page)
    {
        // clear out any old content
        clearContent();

        // clear out any lingering dialog content
        clearDialog();

        // show the header for everything except the landing pages
        _header.setVisible(Pages.LANDING != page);

        // make a note of our current page and create our iframe
        _page = page;
        _iframe = new Frame("/gwt/" + DeploymentConfig.version + "/" + _page.getPath() + "/");
        _iframe.setStyleName("pageIFrame");

        // select the appropriate header tab
        _header.selectTab(page.getTab());

        int contentTop = 0;
        String contentWidth = null, contentHeight = null;
        switch (page) {
        case LANDING:
            closeClient(); // no client on the landing page
            // content takes up whole page
            contentWidth = "100%";
            contentHeight = "100%";
            contentTop = 0;

            // the content is just the supplied widget, no extra bits
            _content = _iframe;
            break;

        case WORLD: {
            Args args = new Args();
            args.setToken(_pageToken);
            String action = args.get(0, "s1");
            if (action.startsWith("s")) {
                String sceneId = action.substring(1);
                if (args.getArgCount() <= 1) {
                    displayWorldClient("sceneId=" + sceneId, null);
                } else {
                    // if we have sNN-extra-args we want the close button to use just "sNN"
                    displayWorldClient(
                        "sceneId=" + sceneId + "&page=" + Args.compose((Object[])args.splice(1)),
                        Pages.WORLD.getPath() + "-s" + sceneId);
                }

            } else if (action.equals("game")) {
                // display a game lobby or enter a game (action_gameId_gameOid)
                displayGame(args.get(1, ""), args.get(2, 0), args.get(3, -1));

            } else if (action.startsWith("g")) {
                // go to a specific group's scene group
                displayWorldClient("groupHome=" + action.substring(1), null);

            } else if (action.startsWith("m")) {
                // go to a specific member's home
                displayWorldClient("memberHome=" + action.substring(1), null);

            } else if (action.startsWith("c")) {
                // join a group chat
                displayWorldClient("groupChat=" + action.substring(1), null);

            } else if (action.startsWith("h")) {
                // go to our home
                displayWorldClient("memberHome=" + CShell.getMemberId(), null);
            }
            break;
        }

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
            _iframe.setWidth(contentWidth);
            _iframe.setHeight((Window.getClientHeight() - HEADER_HEIGHT) + "px");
            content.add(_iframe);
            _content = content;
            break;
        }

        // on LANDING, we ddon't listen for resize as the iframe is height 100%, otherwise do
        setWindowResizerEnabled(page != Pages.LANDING);

        // size, add and position the content
        if (_content != null) {
            _content.setWidth(contentWidth);
            _content.setHeight(contentHeight);
            RootPanel.get(PAGE).add(_content);
            RootPanel.get(PAGE).setWidgetPosition(_content, 0, contentTop);

            // activate our close button if we have a client
            if (_bar != null) {
                _bar.setCloseVisible(FlashClients.clientExists());
            }
        }
    }

    protected void clearContent ()
    {
        if (_content != null) {
            _bar = null;
            RootPanel.get(PAGE).remove(_content);
            _content = null;
        }
    }

    protected void displayWorldClient (String args, String closeToken)
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
        _header.setVisible(true);
        _header.selectTab(null);

        // finally actually display the client
        WorldClient.displayFlash(args, new WorldClient.PanelProvider() {
            public Panel get () {
                if (_client != null) {
                    RootPanel.get(PAGE).remove(_client);
                    _client = null;
                }

                if (Window.getClientHeight() < (NAVI_HEIGHT + CLIENT_HEIGHT)) {
                    _client = new ScrollPanel();
                } else {
                    _client = new SimplePanel();
                }
                _client.setWidth("100%");
                _client.setHeight((Window.getClientHeight() - NAVI_HEIGHT) + "px");
                RootPanel.get(PAGE).add(_client);
                RootPanel.get(PAGE).setWidgetPosition(_client, 0, NAVI_HEIGHT);

                return _client;
            }
        });
    }

    protected void displayGame (final String action, int gameId, final int gameOid)
    {
    	// if we are neither logged in nor have an assigned guest id, we need one
    	boolean assignGuestId = (CShell.getMemberId() == 0);
        // load up the information needed to launch the game
        _usersvc.loadLaunchConfig(gameId, assignGuestId, new MsoyCallback<LaunchConfig>() {
            public void onSuccess (LaunchConfig result) {
                launchGame(result, gameOid, action);
            }
        });
    }

    protected void launchGame (final LaunchConfig config, final int gameOid, String action)
    {
    	// if we were assigned a guest id, make it known to everyone
        if (config.guestId != 0) {
            CShell.frame.dispatchEvent(new GotGuestIdEvent(config.guestId));
        }

        // configure our world client with a default host and port in case we're first to the party
        WorldClient.setDefaultServer(config.groupServer, config.groupPort);

        switch (config.type) {
        case LaunchConfig.FLASH_IN_WORLD:
            displayWorldClient("worldGame=" + config.gameId, null);
            break;

        case LaunchConfig.FLASH_LOBBIED:
            if (gameOid <= 0) {
                String hostPort = "&ghost=" + config.gameServer + "&gport=" + config.gamePort;
                if (action.equals("m") || action.equals("f") || action.equals("s")) {
                    displayWorldClient(
                        "playNow=" + config.gameId + "&mode=" + action + hostPort, null);
                } else {
                    displayWorldClient("gameLobby=" + config.gameId + hostPort, null);
                }
            } else {
                displayWorldClient("gameLocation=" + gameOid, null);
            }
            break;

        case LaunchConfig.JAVA_FLASH_LOBBIED:
        case LaunchConfig.JAVA_SELF_LOBBIED:
            if (config.type == LaunchConfig.JAVA_FLASH_LOBBIED && gameOid <= 0) {
                displayWorldClient("gameLobby=" + config.gameId, null);

            } else {
                // clear out the client as we're going into Java land
                closeClient();

                // prepare a command to be invoked once we know Java is loaded
                _javaReadyCommand = new Command() {
                    public void execute () {
                        displayJava(config, gameOid);
                    }
                };

                // stick up a loading message and the HowdyPardner Java applet
                FlowPanel bits = new FlowPanel();
                bits.setStyleName("javaLoading");
                bits.add(new Label("Loading game..."));

                String hpath = "/clients/" + DeploymentConfig.version + "/howdy.jar";
                bits.add(WidgetUtil.createApplet("game", config.getGameURL(hpath),
                                                 "com.threerings.msoy.client.HowdyPardner",
                                                 "100", "10", true, new String[0]));
                // TODO
                // setContent(bits);
            }
            break;

//         case LaunchConfig.FLASH_SOLO:
//             setFlashContent(
//                     config.name, FlashClients.createSoloGameDefinition(config.gameMediaPath));
//             break;

//         case LaunchConfig.JAVA_SOLO:
//             setContent(config.name, new Label("Not yet supported"));
//             break;

        default:
            CShell.log("Requested to display unsupported game type " + config.type + ".");
            break;
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
        case ADD_NAV_LINK:
            addNavLink(args[0], Enum.valueOf(Pages.class, args[1]), args[2]);
            return null;
        case NAVIGATE_TO:
            navigateTo(args[0]);
            return null;
        case NAVIGATE_REPLACE:
            navigateReplace(args[0]);
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
                    if (_client != null) {
                        _client.setHeight((Window.getClientHeight() - NAVI_HEIGHT) + "px");
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
     * If a tracking cookie doesn't already exist, creates a brand new one
     * with the supplied referral info and a brand new tracking number.
     * Also tells the server to log this as an event.
     */
    // FIXME ROBERT: delete me
    protected void maybeCreateReferral (String affiliate, String vector, String creative)
    {
        if (! TrackingCookie.exists()) {
            ReferralInfo ref =
                ReferralInfo.makeInstance(
                    affiliate, vector, creative, ReferralInfo.makeRandomTracker());
            TrackingCookie.save(ref, false);
            _membersvc.trackReferralCreation(ref, new AsyncCallback<Void>() {
                public void onSuccess (Void result) {
                    // noop
                }
                public void onFailure (Throwable caught) {
                    CShell.log("Failed to send referral creation to server.", caught);
                }
            });
            CShell.log("Created a new ReferralInfo: " + ref);
        }
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

    protected void displayJava (LaunchConfig config, int gameOid)
    {
// TODO: all this information needs to be passed up to the Frame, so maybe the frame should just
// take care of all of this...
//         String[] args = new String[] {
//             "game_id", "" + config.gameId, "game_oid", "" + gameOid,
//             "server", config.gameServer, "port", "" + config.gamePort,
//             "authtoken", (CWorld.ident == null) ? "" : CWorld.ident.token };
//         String gjpath = "/clients/" + DeploymentConfig.version + "/" +
//             (config.lwjgl ? "lwjgl-" : "") + "game-client.jar";
//         WorldClient.displayJava(
//             WidgetUtil.createApplet(
//                 // here we explicitly talk directly to our game server (not via the public facing
//                 // URL which is a virtual IP) so that Java's security policy works
//                 "game", config.getGameURL(gjpath) + "," + config.getGameURL(config.gameMediaPath),
//                 "com.threerings.msoy.game.client." + (config.lwjgl ? "LWJGL" : "") + "GameApplet",
//                 // TODO: allow games to specify their dimensions in their config
//                 "100%", "600", false, args));
    }

    protected void javaReady ()
    {
        if (_javaReadyCommand != null) {
            DeferredCommand.addCommand(_javaReadyCommand);
            _javaReadyCommand = null;
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
            @client.util.Link::goFromFlash(Ljava/lang/String;Ljava/lang/String;)(page, args);
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
        $wnd.howdyPardner = function () {
            entry.@client.frame.FrameEntryPoint::javaReady()();
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
    protected BorderedDialog _dialog;

    /** If the user arrived via an invitation, we'll store that here during their session. */
    protected Invitation _activeInvite;

    /** Handles window resizes. */
    protected WindowResizeListener _resizer;

    /** Used to talk to Google Analytics. */
    protected Analytics _analytics = new Analytics();

    /** A command to be run when Java reports readiness. */
    protected Command _javaReadyCommand;

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final WebMemberServiceAsync _membersvc = (WebMemberServiceAsync)
        ServiceUtil.bind(GWT.create(WebMemberService.class), WebMemberService.ENTRY_POINT);
    protected static final WebUserServiceAsync _usersvc = (WebUserServiceAsync)
        ServiceUtil.bind(GWT.create(WebUserService.class), WebUserService.ENTRY_POINT);

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
