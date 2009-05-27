//
// $Id$

package client.frame;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.gwt.util.CookieUtil;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.VisitorInfo;
import com.threerings.msoy.web.gwt.ABTestCard;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.CookieNames;
import com.threerings.msoy.web.gwt.Invitation;
import com.threerings.msoy.web.gwt.LaunchConfig;
import com.threerings.msoy.web.gwt.MemberCard;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.SessionData;
import com.threerings.msoy.web.gwt.WebMemberService;
import com.threerings.msoy.web.gwt.WebMemberServiceAsync;
import com.threerings.msoy.web.gwt.WebUserService;
import com.threerings.msoy.web.gwt.WebUserServiceAsync;

import client.images.frame.FrameImages;
import client.shell.BrowserTest;
import client.shell.CShell;
import client.shell.LandingTestCookie;
import client.shell.Session;
import client.shell.ShellMessages;
import client.ui.BorderedDialog;
import client.util.ArrayUtil;
import client.util.FlashClients;
import client.util.FlashVersion;
import client.util.Link;
import client.util.InfoCallback;
import client.util.NoopAsyncCallback;
import client.util.ServiceUtil;
import client.util.StringUtil;
import client.util.events.FlashEvent;
import client.util.events.FlashEvents;
import client.util.events.NameChangeEvent;

/**
 * Handles the outer shell of the Whirled web application. Loads pages into an iframe and also
 * handles displaying the Flash client.
 */
public class FrameEntryPoint
    implements EntryPoint, ValueChangeHandler<String>, Session.Observer, client.shell.Frame
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
        History.addValueChangeHandler(this);
        _currentToken = History.getToken();

        // validate our session which will dispatch a didLogon or didLogoff
        Session.validate();

        // create our header
        _header = new FrameHeader(new ClickHandler() {
            public void onClick (ClickEvent event) {
                if (_closeToken != null) {
                    closeContent();
                } else if (CShell.isGuest()) {
                    History.newItem("");
                } else {
                    Link.go(Pages.WORLD, "m" + CShell.getMemberId());
                }
            }
        });

        // create our frame layout
        _layout = Layout.getLayout(_header, new ClickHandler() {
            public void onClick (ClickEvent event) {
                // put the client in in minimized state
                String args = "memberHome=" + CShell.getMemberId() + "&mini=true";
                _closeToken = Pages.WORLD.makeToken("h");
                if (_bar != null) {
                    _bar.setCloseVisible(true);
                }
                WorldClient.displayFlash(args, _layout.getClientProvider());
            }
        });

        // clear out the loading HTML so we can display a browser warning or load Whirled
        DOM.setInnerHTML(RootPanel.get(LOADING).getElement(), "");

        // If the browser is unsupported, hide the page (still being built) and show a warning.
        ClickHandler continueClicked = new ClickHandler() {
            public void onClick (ClickEvent event) {
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

    // from interface ValueChangeHandler
    public void onValueChange (ValueChangeEvent<String> event)
    {
        setToken(event.getValue());
    }

    public void setToken (String token)
    {
        _prevToken = _currentToken;
        _currentToken = token;

        Pages page;
        Args args;
        try {
            page = Pages.fromHistory(token);
            args = Args.fromHistory(token);
        } catch (Exception e) {
            // on bogus or missing URLs, go to landing page for guests or world-places for members
            if (CShell.isGuest()) {
                page = Pages.LANDING;
                args = Args.fromToken("");
            } else {
                page = Pages.WORLD;
                args = Args.fromToken("places");
            }
        }

        CShell.log("Displaying page", "page", page, "args", args);

        // do some special processing if this is an invitation link
        if (page == Pages.ME && args.get(0, "").equals("i") && CShell.isGuest()) {
            // load up the invitation information and save it
            String inviteId = args.get(1, "");
            if (_activeInvite == null || !_activeInvite.inviteId.equals(inviteId)) {
                _membersvc.getInvitation(inviteId, true, new InfoCallback<Invitation>() {
                    public void onSuccess (Invitation invite) {
                        _activeInvite = invite;
                    }
                });
            }
            // and send them to the landing page
            Link.go(Pages.LANDING, "");
            return;
        }

        // if we have no account cookie (which means we've seen someone on this computer before),
        // force the creation of our visitor info because we're very probably a real new user
        boolean newUser = StringUtil.isBlank(CookieUtil.get(CookieNames.WHO));
        if (newUser) {
            getVisitorInfo(); // creates a visitorId and reports it
        }

        // do different things for new users on landing
        if (page == Pages.LANDING && args.get(0, "").equals("") && newUser) {
            ABTestCard test = LandingTestCookie.getTest("2009 05 landing take 2");
            int landingGroup = test == null ? -1 : test.getGroup(getVisitorInfo());
            switch (landingGroup) {
            case 1:  // group A: normal
                break;
            case 2:  // group B: go home and see hpg
                page = Pages.WORLD;
                args = Args.fromToken("hplaces");
                break;
            case 3:  // group C: register NOW + force validate
                page = Pages.LANDACC;
                args = Args.fromToken("reg");
                break;
            case 4: // group D: compact landing page
                page = Pages.LANDING;
                args = Args.fromToken("compact");
                break;
            }
            // log the result to the server
            if (landingGroup > 0) {
                _membersvc.logLandingABTestGroup(getVisitorInfo(), test.name, landingGroup,
                    new NoopAsyncCallback());
                CShell.log("Displaying alternate page", "page", page, "args", args,
                    "group", landingGroup);
            }
        }

        // recreate the page token which we'll pass through to the page (or if it's being loaded
        // for the first time, it will request in a moment with a call to getPageToken)
        _pageToken = args.recompose(0).toToken();

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

        // let the frame header update promo text
        _header.tickPromo();

        // convert the page to GA format and report it to Google Analytics
        _analytics.report(args.toPath(page));
    }

    // from interface Session.Observer
    public void didLogon (SessionData data)
    {
        // update the world client to relogin (this will NOOP if we're logging in now because Flash
        // just told us to do so)
        WorldClient.didLogon(data.creds);

        // reboot the flash client (which will put them back where they are but logged in as their
        // newly registered self)
        if (FlashClients.clientExists() && data.source == SessionData.Source.CREATE) {
            rebootFlashClient();
        }

        // now that we know we're a member, we can add our "open home in minimized mode" icon
        // (which may get immediately removed if we're going directly into the world)... don't do
        // it if we have registered with validation so that the "ok, now check your email" screen
        // stays as-is
        if (data.source != SessionData.Source.VALIDATED_CREATE) {
            _layout.addNoClientIcon();
        }

        if (data.source == SessionData.Source.CREATE) {
            Link.go(Pages.PEOPLE, "confprof"); // send them to step 2: configure profile
        } else if (data.source == SessionData.Source.VALIDATED_CREATE) {
            // don't do any redirection in this case, we are waiting for them to validate
        } else if (_page == Pages.LANDING || (_page == Pages.ACCOUNT && _prevToken.equals(""))) {
            Link.go(Pages.WORLD, "places");
        } else if (_page == Pages.ACCOUNT) {
            History.back(); // go back to where we were
        } else if (_page != null) {
            setPage(_page); // reloads the current page
        } else {
            setToken(_currentToken);
        }
    }

    // from interface Session.Observer
    public void didLogoff ()
    {
        // clear out any current page
        _page = null;
        // reload the current page (preserving our previous page token)
        String prevToken = _prevToken;
        setToken(_currentToken);
        _prevToken = prevToken;
        // close the Flash client if it's open
        closeClient(true);
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
    public void addNavLink (String label, Pages page, Args args, int position)
    {
        _bar.addContextLink(label, page, args, position);
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
            setToken(_currentToken);
        } else {
            History.back();
            History.newItem(token);
        }
    }

    // from interface Frame
    public void closeClient ()
    {
        closeClient(false);
    }

    // from interface Frame
    public void closeContent ()
    {
        // let the Flash client know that it's being unminimized
        WorldClient.setMinimized(false);

        // clear out the content
        clearContent(true);

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
            @Override protected void onClosed (boolean autoClosed) {
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
    public void logoff ()
    {
        Session.didLogoff();
    }

    // from interface Frame
    public void emailUpdated (String address, boolean validated)
    {
        Session.emailUpdated(address, validated);
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

    // from interface Frame
    public VisitorInfo getVisitorInfo ()
    {
        return Session.frameGetVisitorInfo();
    }

    // from interface Frame
    public void reportTestAction (String test, String action)
    {
        CShell.log("Reporting test action", "test", test, "action", action);
        _membersvc.trackTestAction(test, action, getVisitorInfo(), new NoopAsyncCallback());
    }

    protected void setPage (Pages page)
    {
        // clear out any old content
        clearContent(page == Pages.WORLD);

        // clear out any lingering dialog content
        clearDialog();

        // show the header for pages that report a tab of which they are a part
        _header.selectTab(page.getTab());

        // make a note of our current page
        _page = page;

        // if we're displaying a world page, that's special
        if (page == Pages.WORLD) {
            displayWorld(_pageToken);
            return;
        }

        // create our iframe
        _iframe = new Frame("/gwt/" + DeploymentConfig.version + "/" + _page.getPath() + "/");
        _iframe.setStyleName("pageIFrame");

        // if we're on a headerless page, we need to close the client
        if (page.getTab() == null) {
            closeClient();
        } else {
            _bar = TitleBar.create(page.getTab(), new ClickHandler() {
                public void onClick (ClickEvent event) {
                    closeContent();
                }
            });
            _bar.setCloseVisible(FlashClients.clientExists());
        }
        _layout.setContent(_bar, _iframe);
    }

    protected void clearContent (boolean restoreClient)
    {
        if (_layout.closeContent(restoreClient)) {
            // restore the title to the last thing flash asked for
            setTitle(_closeTitle);
        }
        _iframe = null;
        _bar = null;
    }

    protected void closeClient (boolean didLogoff)
    {
        WorldClient.clientWillClose();
        _closeToken = null;
        _closeTitle = null;

        if (_bar != null) {
            _bar.setCloseVisible(false);
        }

        if (_layout.closeClient()) {
            // if we just logged off, go to the logoff page
            if (didLogoff) {
                Link.go(Pages.ACCOUNT, "logoff");

            // if we're on a "world" page, go to a landing page
            } else if (_currentToken != null &&
                       (_currentToken.startsWith(Pages.WORLD.getPath()) ||
                        _currentToken.equals(""))) {
                if (_currentToken.indexOf("game") != -1) {
                    // if we were in a game, go to the games page
                    Link.go(Pages.GAMES, "");
                } else if (CShell.isGuest()) {
                    // if we're a guest, go to the rooms page
                    Link.go(Pages.ROOMS, "");
                } else {
                    // otherwise go to the ME page
                    Link.go(Pages.ME, "");
                }
            }
        }
    }

    protected void displayWorld (String pageToken)
    {
        Args args = Args.fromToken(pageToken);

        String action = args.get(0, "");
        if (action.startsWith("s")) {
            String sceneId = action.substring(1);
            if (args.getArgCount() <= 1) {
                displayWorldClient("sceneId=" + sceneId, null);
            } else {
                // if we have sNN-extra-args we want the close button to use just "sNN"
                displayWorldClient("sceneId=" + sceneId + "&page=" + args.get(1, "") +
                                   "&args=" + args.recompose(2),
                                   Pages.WORLD.getPath() + "-s" + sceneId);
            }

        } else if (action.equals("game")) {
            // display a game lobby or enter a game (action_gameId_otherid1_token_otherid2)
            displayGame(args.get(1, ""), args.get(2, 0), args.get(3, 0), args.get(4, ""),
                        args.get(5, 0));

        } else if (action.equals("tour")) {
            displayWorldClient("tour=true", null);

        } else if (action.startsWith("g")) {
            // go to a specific group's scene group
            displayWorldClient("groupHome=" + action.substring(1), null);

        } else if (action.startsWith("m")) {
            // go to a specific member's home
            displayWorldClient("memberHome=" + action.substring(1), null);

        } else if (action.startsWith("c")) {
            // join a group chat
            displayWorldClient("groupChat=" + action.substring(1), null);

        } else if (action.equals("h")) {
            // go to our home
            displayWorldClient("memberHome=" + CShell.getMemberId(), null);

        } else if (action.equals("hplaces")) {
            // just logon and show the myplaces dialog, don't go anywhere
            displayWorldClient("myplaces=true", null);

        } else { // (action == "places" or anything else)
            // just logon and go home for now
            displayWorldClient("memberHome=" + CShell.getMemberId(), null);
        }
    }

    protected void displayWorldClient (String args, String closeToken)
    {
        // note the current history token so that we can restore it if needed
        _closeToken = (closeToken == null) ? _currentToken : closeToken;

        // finally actually display the client
        WorldClient.displayFlash(args, _layout.getClientProvider());
    }

    protected void displayGame (final String action, int gameId, final int otherId1,
                                final String token, final int otherId2)
    {
        // load up the information needed to launch the game
        _usersvc.loadLaunchConfig(gameId, new InfoCallback<LaunchConfig>() {
            public void onSuccess (LaunchConfig result) {
                launchGame(result, action, otherId1, token, otherId2);
            }
        });
    }

    protected void launchGame (final LaunchConfig config, String action, final int otherId1,
                               String token, final int otherId2)
    {
        // configure our world client with a default host and port in case we're first to the party
        WorldClient.setDefaultServer(config.groupServer, config.groupPort);

        // if we're launching a chromeless game from inside the Facebook App we do some hackery
        FlashClients.setChromeless(action.equals("pc"));

        // sanitize our token
        token = (token == null) ? "" : token;

        String args;
        switch (config.type) {
        case LaunchConfig.FLASH_IN_WORLD:
            args = "worldGame=" + config.gameId;
            if (action.equals("j")) {
                args += "&inviteToken=" + token + "&inviterMemberId=" + otherId1 +
                    "&gameRoomId=" + otherId2;
            } else {
                args += "&gameRoomId=" + config.sceneId;
            }
            displayWorldClient(args, null);
            break;

        case LaunchConfig.FLASH_LOBBIED:
            String hostPort = "&ghost=" + config.gameServer + "&gport=" + config.gamePort;
            args = "gameId=" + config.gameId;

            // "g" means we're going right into an already running game
            if (action.equals("g")) {
                args += "&gameOid=" + otherId1;

            // "j" is from a game invite
            } else if (action.equals("j")) {
                args += "&inviteToken=" + token + "&inviterMemberId=" + otherId1;

            // everything else ("p", "pc" and "i" and legacy codes) means 'play now'
            } else if (otherId1 != 0) {
                args += "&playerId=" + otherId1;
            }
            displayWorldClient(args + hostPort, null);
            break;

        case LaunchConfig.JAVA_FLASH_LOBBIED:
        case LaunchConfig.JAVA_SELF_LOBBIED:
            if (config.type == LaunchConfig.JAVA_FLASH_LOBBIED && otherId1 <= 0) {
                displayWorldClient("gameId=" + config.gameId, null);

            } else {
                // clear out the client as we're going into Java land
                closeClient();

                // prepare a command to be invoked once we know Java is loaded
                _javaReadyCommand = new Command() {
                    public void execute () {
                        displayJava(config, otherId1);
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
//                     config.name, FlashClients.createSoloGameDefinition(config.clientMediaPath));
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
            addNavLink(args[0], Enum.valueOf(Pages.class, args[1]), Args.fromToken(args[2]),
                       Integer.parseInt(args[3]));
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
            dispatchDidLogon(SessionData.unflatten(ArrayUtil.toIterator(args)));
            return null;
        case LOGOFF:
            logoff();
            return null;
        case EMAIL_UPDATED:
            emailUpdated(args[0], Boolean.parseBoolean(args[1]));
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
        case GET_VISITOR_INFO:
            return getVisitorInfo().flatten().toArray(new String[0]);
        case TEST_ACTION:
            reportTestAction(args[0], args[1]);
            return null;
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
//                 "game", config.getGameURL(gjpath) + "," + config.getGameURL(config.clientMediaPath),
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

    protected String getVisitorId ()
    {
        return getVisitorInfo().id;
    }

    protected void setTitleFromFlash (String title)
    {
        // if we're displaying content currently, don't let flash mess with the title
        if (!_layout.haveContent()) {
            setTitle(title);
        }
        _closeTitle = title;
    }

    protected void setPermaguestInfo (String name, String token)
    {
        // the server has created a permaguest account for us via flash, store the cookies
        CShell.log("Got permaguest info from flash", "name", name, "token", token);
        Session.conveyLoginFromFlash(token);
    }

    protected void refreshDisplayName ()
    {
        _membersvc.getMemberCard(CShell.getMemberId(), new AsyncCallback<MemberCard>() {
            public void onFailure (Throwable caught) {
            }

            public void onSuccess (MemberCard result) {
                if (result != null) {
                    dispatchEvent(new NameChangeEvent(result.name.toString()));
                }
            }
        });
    }

    protected void rebootFlashClient ()
    {
        WorldClient.rebootFlash(_layout.getClientProvider());
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
            entry.@client.frame.FrameEntryPoint::setTitleFromFlash(Ljava/lang/String;)(title);
        };
        $wnd.displayPage = function (page, args) {
            @client.util.Link::goFromFlash(Ljava/lang/String;Ljava/lang/String;)(page, args);
        };
        $wnd.clearClient = function () {
             entry.@client.frame.FrameEntryPoint::deferredCloseClient()();
        };
        $wnd.getVisitorId = function () {
             return entry.@client.frame.FrameEntryPoint::getVisitorId()();
        };
        $wnd.toggleClientHeight = function () {
            @client.util.FlashClients::toggleClientFullHeight()();
        }
        $wnd.triggerFlashEvent = function (eventName, args) {
            entry.@client.frame.FrameEntryPoint::triggerEvent(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(eventName, args);
        }
        $wnd.howdyPardner = function () {
            entry.@client.frame.FrameEntryPoint::javaReady()();
        }
        $wnd.setPermaguestInfo = function (name, token) {
            entry.@client.frame.FrameEntryPoint::setPermaguestInfo(Ljava/lang/String;Ljava/lang/String;)(name, token);
        }
        $wnd.refreshDisplayName = function () {
            entry.@client.frame.FrameEntryPoint::refreshDisplayName()();
        }
        $wnd.rebootFlashClient = function () {
            entry.@client.frame.FrameEntryPoint::rebootFlashClient()();
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
            if (frame.contentWindow && frame.contentWindow.triggerEvent) {
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

    protected Pages _page;
    protected String _currentToken = "";
    protected String _pageToken = "";
    protected String _prevToken = "";
    protected String _closeToken;
    protected String _closeTitle;

    protected FrameHeader _header;
    protected Layout _layout;
    protected TitleBar _bar;
    protected Frame _iframe;
    protected BorderedDialog _dialog;

    /** If the user arrived via an invitation, we'll store that here during their session. */
    protected Invitation _activeInvite;

    /** Used to talk to Google Analytics. */
    protected Analytics _analytics = new Analytics();

    /** Handles publishing info to external feeds. It registers event listeners in its constructor,
     * so we don't ever need to actually talk to this instance. */
    protected ExternalFeeder _feeder = new ExternalFeeder();

    /** A command to be run when Java reports readiness. */
    protected Command _javaReadyCommand;

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final FrameImages _images = (FrameImages)GWT.create(FrameImages.class);
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
        "fbhelper", "/js/facebook.js",
    };
}
