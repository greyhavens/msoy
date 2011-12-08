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
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.util.CookieUtil;
import com.threerings.gwt.util.StringUtil;

import com.threerings.msoy.data.all.VisitorInfo;
import com.threerings.msoy.facebook.gwt.FacebookService;
import com.threerings.msoy.facebook.gwt.FacebookServiceAsync;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.ClientMode;
import com.threerings.msoy.web.gwt.CookieNames;
import com.threerings.msoy.web.gwt.Embedding;
import com.threerings.msoy.web.gwt.Invitation;
import com.threerings.msoy.web.gwt.MemberCard;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.SessionData;
import com.threerings.msoy.web.gwt.WebMemberService;
import com.threerings.msoy.web.gwt.WebMemberServiceAsync;
import com.threerings.msoy.web.gwt.WebUserService;
import com.threerings.msoy.web.gwt.WebUserServiceAsync;

import client.frame.FrameNav.FrameId;
import client.shell.CShell;
import client.shell.Frame;
import client.shell.ScriptSources;
import client.shell.Session;
import client.shell.ThemedStylesheets;
import client.ui.BorderedDialog;
import client.util.ArrayUtil;
import client.util.FlashClients;
import client.util.FlashVersion;
import client.util.InfoCallback;
import client.util.Link;
import client.util.NoopAsyncCallback;
import client.util.events.FlashEvent;
import client.util.events.FlashEvents;
import client.util.events.NameChangeEvent;

/**
 * Handles the outer shell of the Whirled web application. Loads pages into an iframe and also
 * handles displaying the Flash client.
 */
public class FrameEntryPoint
    implements EntryPoint, ValueChangeHandler<String>, Session.Observer, Frame, FrameNav.Listener
{
    /**
     * Creates the new frame entry point.
     */
    public FrameEntryPoint ()
    {
        // listen for trophy events to publish to facebook
        TrophyFeeder.listen();
    }

    // from interface EntryPoint
    public void onModuleLoad ()
    {
        // set up our CShell singleton
        CShell.init(this);

        // our main frame never scrolls
        Window.enableScrolling(false);

        // listen for logon/logoff
        Session.addObserver(this);

        // wire ourselves up to the history-based navigation mechanism
        History.addValueChangeHandler(this);
        _currentToken = History.getToken();

        _embedding = Embedding.extract(Args.fromHistory(_currentToken));
        CShell.log("Loading module", "embedding", _embedding);

        // Kick off the rest of the client after receiving an embedding mode from the server
        _usersvc.getEmbedding(new InfoCallback<Embedding>() {
            public void onSuccess (Embedding embedding) {
                init(embedding);
            }
        });

        // set up the callbacks that our flash clients can call
        configureCallbacks();

        _themes = new ThemedStylesheets();

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

    @Override // from FrameNav.Listener
    public void onLogoClick ()
    {
        if (CShell.isGuest()) {
            History.newItem("");
        } else {
            Link.go(Pages.WORLD, "m" + CShell.getMemberId());
        }
    }

    @Override // from FrameNav.Listener
    public void onClientClosed (boolean didLogoff)
    {
        // if we just logged off, go to the logoff page
        if (didLogoff) {
            Link.go(Pages.ACCOUNT, "logoff");

        // if we're on a "world" page, go to a landing page
        } else if (_currentToken != null &&
                   (_currentToken.startsWith(Pages.WORLD.makeToken()) ||
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

    @Override // from FrameNav.Listener
    public void onContentClosed (String lastFlashToken)
    {
        History.newItem(lastFlashToken);
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

        // scrub any cookie-like arguments
        Embedding.extract(args);

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
        if (StringUtil.isBlank(CookieUtil.get(CookieNames.WHO))) {
            getVisitorInfo(); // creates a visitorId and reports it
        }

        if (page != _nav.getPage(FrameId.MAIN)) {
            // clear out any lingering dialog content
            clearDialog();
        }

        _nav.go(FrameId.MAIN, page, args.recompose(0).toToken());

        // report the page visit
        reportPageVisit(page, args);
    }

    // from interface Session.Observer
    public void didLogon (SessionData data)
    {
        // if they just registered, reboot the flash client (which will put them back where they
        // are but logged in as their newly registered self)
        if (FlashClients.clientExists() && data.group != SessionData.Group.NONE) {
            _nav.rebootFlashClient();
        }

        Pages curPage = _nav.getPage(FrameId.MAIN);

        // MDB removed data.originatedInFlash for some presumably enlightened reason in 2/09, then
        // added the isHeaderless() check below in 6/09... the two together cause permaguests
        // going into a game to redirect to #world-places. So... to fix it, kludgily just don't do
        // any redirects if we are currently on the world page.
        // TODO: make this more robust, are there any other conditions where a login would occur
        // while the WORLD page is open? If so, we may need to resurrect data.originatedInFlash
        // and check that instead
        if (curPage == Pages.WORLD) {
            return;
        }

        if (data.group != SessionData.Group.NONE) {
            Link.go(Pages.PEEPLESS, "confprof"); // send them to configure profile

        } else if (isHeaderless()) {
            // this takes care of all the landing pages
            Link.go(Pages.WORLD, "places");

        } else if (curPage == Pages.ACCOUNT) {
            if (_prevToken.equals("") || _prevToken.startsWith(Pages.LANDING.makeToken())) {
                // if we're logging in without a previous page, OR we're logging in from a landing
                // page, hop forward into the world
                Link.go(Pages.WORLD, "places");
            } else {
                // otherwise just go back to where we were
                History.back();
            }

        } else if (curPage != null) {
            _nav.reload(); // reloads the current page

        } else {
            setToken(_currentToken);
        }
    }

    // from interface Session.Observer
    public void didLogoff ()
    {
        _nav.closeContent();

        // reload the current page (preserving our previous page token)
        String prevToken = _prevToken;
        setToken(_currentToken);
        _prevToken = prevToken;
    }

    // from interface Frame
    public void setTitle (String title)
    {
        _nav.setTitle(title, false);
    }

    // from interface Frame
    public void addNavLink (String label, Pages page, Args args, int position)
    {
        _nav.addLink(label, page, args, position);
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
        _nav.closeClient();
    }

    // from interface Frame
    public void closeContent ()
    {
        _nav.closeContent();
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

        // forward the event to our page frame
        _nav.forwardEvent(event);
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

    // from interface Frame
    public int getAppId ()
    {
        return _embedding.appId;
    }

    // from interface Frame
    public Embedding getEmbedding ()
    {
        return _embedding;
    }

    // from interface Frame
    public boolean isHeaderless ()
    {
        return _nav.isHeaderless();
    }

    // from interface Frame
    public void openBottomFrame (String token)
    {
        Pages page = Pages.fromHistory(token);
        String pageToken = Args.fromHistory(token).toToken();
        _nav.go(FrameId.BOTTOM, page, pageToken);
    }

    // from interface Frame
    public int getThemeId ()
    {
        return _themes.getThemeId();
    }

    protected void init (Embedding embedding)
    {
        // assign our client mode and app id if the server gave them to us, otherwise use defaults
        if (embedding != null) {
            CShell.log("Received new embedding", "embedding", embedding);
            _embedding = embedding;
        }

        // load up various JavaScript sources
        ScriptSources.inject(_embedding.appId);

        // if we're a brand new visitor, we might need to supply a entry vector since we know
        // what the server can't know, i.e. the history token
        Session.frameGetVisitorInfo();

        // validate our session which will dispatch a didLogon or didLogoff
        Session.validate();

        // create our ...
        _nav = new FrameNav(_embedding, this);
    }

    /**
     * Handles a variety of methods called by our iframed page.
     */
    protected String[] frameCall (String callStr, String frameId, String[] args)
    {
        Calls call = Calls.valueOf(callStr);
        FrameId frame = FrameId.valueOf(frameId);
        switch (call) {
        case SET_TITLE:
            // only the main frame can set the title
            if (frame == FrameId.MAIN) {
                setTitle(args[0]);
            }
            return null;
        case ADD_NAV_LINK:
            addNavLink(args[0], Pages.valueOf(args[1]), Args.fromToken(args[2]),
                       Integer.parseInt(args[3]));
            return null;
        case NAVIGATE_TO:
            // TODO: can the bottom frame navigate itself?
            navigateTo(args[0]);
            return null;
        case NAVIGATE_REPLACE:
            // TODO: bottom frame navigation
            if (frame == FrameId.MAIN) {
                navigateReplace(args[0]);
            }
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
            return new String[] { _nav.getToken(frame) };
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
        case GET_EMBEDDING:
            return _embedding.flatten();
        case IS_HEADERLESS:
            return new String[] { String.valueOf(isHeaderless()) };
        case OPEN_BOTTOM_FRAME:
            openBottomFrame(args[0]);
            return null;
        case GET_THEME_ID:
            return new String[] { String.valueOf(getThemeId()) };
        case CONTENT_SET:
            _nav.contentSet(frame, Pages.valueOf(args[0]), args[1]);
            return null;
        }
        CShell.log("Got unknown frameCall request [call=" + call + "].");
        return null; // not reached
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

    protected String getVisitorId ()
    {
        return getVisitorInfo().id;
    }

    protected void refreshDisplayName ()
    {
        _membersvc.getMemberCard(CShell.getMemberId(), new AsyncCallback<MemberCard>() {
            public void onFailure (Throwable caught) {
                // nada
            }
            public void onSuccess (MemberCard result) {
                if (result != null) {
                    dispatchEvent(new NameChangeEvent(result.name.toString()));
                }
            }
        });
    }

    protected void trackEvent (String category, String action)
    {
        _analytics.trackEvent(category, action);
    }

    protected void reportPageVisit (Pages page, Args args)
    {
        // convert the page to a url
        String url = args.toPath(page);

        // report it to Google Analytics
        _analytics.report(url);

        // and to Kontagent if we are in a Facebook mode
        if (_embedding.mode.isFacebook()) {
            // TODO: we might be able to use the recommended "client pixel" page reporting if the
            // KAPI secret is not required (question posted to forums). If so, then we'd need to
            // grab the FBID from somewhere, perhaps in the SessionData (superseding the
            // #world-fbgame way of doing it), and build a URL here and poke it into an Image in
            // the title bar or somewhere.
            _fbsvc.trackPageRequest(_embedding.appId, url, new NoopAsyncCallback());
        }
    }

    /**
     * Configures top-level functions that can be called by Flash or an iframed
     * {@link client.shell.Page}.
     */
    protected native void configureCallbacks () /*-{
        var entry = this;
        $wnd.onunload = function (event) {
            var client = $doc.getElementById("asclient");
            if (client) {
                client.onUnload();
            }
            return true;
        };
        $wnd.frameCall = function (action, pageFrameId, args) {
            return entry.@client.frame.FrameEntryPoint::frameCall(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;)(action, pageFrameId, args);
        };
        $wnd.helloWhirled = function () {
             return true;
        };
        $wnd.getClientMode = function () {
            var emb = entry.@client.frame.FrameEntryPoint::getEmbedding()();
            var mode = emb.@com.threerings.msoy.web.gwt.Embedding::mode;
            return mode.@java.lang.Object::toString()();
        }
        $wnd.displayPage = function (page, args) {
            @client.util.Link::goFromFlash(Ljava/lang/String;Ljava/lang/String;)(page, args);
        };
        $wnd.getVisitorId = function () {
             return entry.@client.frame.FrameEntryPoint::getVisitorId()();
        };
        $wnd.triggerFlashEvent = function (eventName, args) {
            entry.@client.frame.FrameEntryPoint::triggerEvent(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(eventName, args);
        };
        $wnd.refreshDisplayName = function () {
            entry.@client.frame.FrameEntryPoint::refreshDisplayName()();
        };
        $wnd.trackEvent = function (category, action) {
            entry.@client.frame.FrameEntryPoint::trackEvent(Ljava/lang/String;Ljava/lang/String;)(category, action);
        };
    }-*/;

    /** MD5 hashes the supplied text and returns the hex encoded hash value. */
    public native static String nmd5hex (String text) /*-{
        return $wnd.hex_md5(text);
    }-*/;

    protected String _currentToken = "", _prevToken = "";

    protected Embedding _embedding;
    protected FrameNav _nav;
    protected BorderedDialog _dialog;
    protected ThemedStylesheets _themes;

    /** If the user arrived via an invitation, we'll store that here during their session. */
    protected Invitation _activeInvite;

    /** Used to talk to Google Analytics. */
    protected Analytics _analytics = new Analytics();

    protected static final WebMemberServiceAsync _membersvc = GWT.create(WebMemberService.class);
    protected static final FacebookServiceAsync _fbsvc = GWT.create(FacebookService.class);
    protected static final WebUserServiceAsync _usersvc = GWT.create(WebUserService.class);

    // constants for our top-level elements
    protected static final String LOADING = "loading";

    /** This vector string represents an email invite */
    protected static final String EMAIL_VECTOR = "emailInvite";
}
