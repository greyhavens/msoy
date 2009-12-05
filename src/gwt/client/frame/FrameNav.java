//
// $Id$

package client.frame;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.threerings.msoy.data.all.LaunchConfig;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Embedding;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.SessionData;
import com.threerings.msoy.web.gwt.WebUserService;
import com.threerings.msoy.web.gwt.WebUserServiceAsync;

import client.shell.CShell;
import client.shell.Session;
import client.shell.ShellMessages;
import client.util.FlashClients;
import client.util.InfoCallback;
import client.util.events.FlashEvent;

/**
 * Encapsulates all of the logic associated with moving between whirled locations. This is intended
 * primarily as one-way communication, i.e. the main module or page modules request all navigation
 * changes. The navigation never changes the token, e.g. via Link.go or History.newItem. There is
 * some very limited communication back to the frame entry point, defined by a listener interface.
 */
public class FrameNav
{
    /** The frames that we manage. */
    public enum FrameId { MAIN, BOTTOM }

    /**
     * Methods to be called on certain navigational events.
     */
    public interface Listener
    {
        /**
         * Called when the user clicks on the logo in the frame header.
         */
        void onLogoClick ();

        /**
         * Called when the flash client is closed.
         * @param didLogoff if set, the client is being closed because of a logoff
         */
        void onClientClosed (boolean didLogoff);

        /**
         * Called when the content closes.
         * @param lastFlashToken if not null, the token that was in effect the last time flash was
         * not minimized
         */
        void onContentClosed (String lastFlashToken);

        /**
         * Called when a Facebook game is entered.
         * @param uid the Facebook user id given by the token
         * @param session the Facebook session id given by the token
         */
        void onEnterFacebookGame (String uid, String session);
    }

    /**
     * Creates a new frame nav.
     * @param embedding how the application is embedded, this does not change
     * @param listener the listener to which navigation events will be dispatched
     */
    public FrameNav (Embedding embedding, Listener listener)
    {
        _embedding = embedding;
        _listener = listener;

        // listen for logon/logoff
        Session.addObserver(new Session.Observer() {
            @Override public void didLogon (SessionData data) {
                // now that we know we're a member, we can add our "open home in minimized mode"
                // icon (which may get immediately removed if we're going directly into the world)
                _layout.addNoClientIcon();
            }
            @Override public void didLogoff () {
                // TODO: this page clear may be needed after all...
                // ...it used to happen just before setToken(_currentToken) in FrameEntryPoint
                // clear out any current page
                //_main.page = null;

                // close the Flash client if it's open
                closeClient(true);
            }
        });

        // create our header
        _header = new FrameHeader(new ClickHandler() {
            public void onClick (ClickEvent event) {
                if (_closeToken != null) {
                    closeContent();
                } else {
                    _listener.onLogoClick();
                }
            }
        });

        // create our frame layout
        _layout = Layout.create(_header, embedding.mode, isFramed(), new ClickHandler() {
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

    }

    /**
     * Navigates a frame to a location.
     * @param frame the id of the frame being navigated
     * @param page the module/mode to navigate to
     * @param token the argument to the module
     */
    public void go (FrameId frame, Pages page, String token)
    {
        if (frame == FrameId.BOTTOM) {
            CShell.log("Opening bottom frame", "token", token);
            if (_bottom.page != page || !token.equals(_bottom.token)) {
                _bottom.page = page;
                _bottom.token = token;
                _bottom.frame = new PageFrame(page, FrameId.BOTTOM.name());
                _layout.setBottomContent(_bottom.frame);
            }
            return;
        }

        if (frame != FrameId.MAIN) {
            throw new IllegalArgumentException();
        }

        // replace the page if necessary
        if (_main.page != page || _main.page == Pages.WORLD) {
            setMainPage(page, token);

        } else {
            // reset our navigation as we're not changing pages but need to give the current page a
            // fresh subnavigation palette
            if (_bar != null) {
                _bar.resetNav();
            }
            _main.token = token;
            _main.frame.setToken(_main.token);
            WorldClient.contentRequested(_main.page, _main.token);
        }
    }

    /**
     * Sets the title of the window and, if appropriate, the title bar. This is normally called
     * when a module if finally loaded via the frame module's page callbacks.
     * @param title the title to set
     * @param fromFlash if the title is being set from flash; if so, the title may be stored and
     *        set later when the content is closed
     */
    public void setTitle (String title, boolean fromFlash)
    {
        // if we're displaying content currently, don't let flash mess with the title
        if (!fromFlash || !_layout.hasContent()) {
            if (title != null && _bar != null) {
                Window.setTitle(title == null ? _cmsgs.bareTitle() : _cmsgs.windowTitle(title));
                _bar.setTitle(title);
            }
        }
        if (fromFlash) {
            _closeTitle = title;
        }
    }

    /**
     * Adds a link to the title bar. This is normally called when a module enters a state that
     * requires extra subnavigation.
     * @param label the label for the link
     * @param page the page to link to
     * @param args the arguments to the page module
     * @param position the position in the row of links to place the new link at
     */
    public void addLink (String label, Pages page, Args args, int position)
    {
        _bar.addContextLink(label, page, args, position);
        _layout.updateTitleBarHeight();
    }

    /**
     * Forwards an event to the current page. This is normally called in response to a flash event.
     * @param event the event to forward
     */
    public void forwardEvent (FlashEvent event)
    {
        if (_main.frame != null) {
            _main.frame.forwardEvent(event);
        }
    }

    /**
     * Returns true if the current page has no header (tabs). For example, the #landing page has
     * no tabs.
     */
    public boolean isHeaderless ()
    {
        return (_main.page != null) && (_main.page.getTab() == null);
    }

    /**
     * Gets the page that a frame currently has open, or null if the frame is not visible.
     * @param frame the frame whose page to get
     * @return the page the frame has open, or null if none
     */
    public Pages getPage (FrameId frame)
    {
        return toFrame(frame).page;
    }

    /**
     * Gets the current token (argument) on a frame.
     * @param frame the frame whose token to get
     * @return the argument on the frame; the result is undefined if
     *         <code>getPage(frame) == null</code>
     */
    public String getToken (FrameId frame)
    {
        return toFrame(frame).token;
    }

    /**
     * Closes the flash client and updates other navigation components such as the close button
     * on the title bar (if visible).
     */
    public void closeClient ()
    {
        closeClient(false);
    }

    /**
     * Closes the content and updates other navigation components such as maximizing the client
     * and hiding the title bar if appropriate.
     */
    public void closeContent ()
    {
        // clear out the content
        clearContent(true);

        // restore the client's URL
        if (_closeToken != null) {
            _listener.onContentClosed(_closeToken);
        }
    }

    /**
     * Reloads the current page. Normally only needed in response to the user logging in, if there
     * is no other appropriate page page to go to.
     */
    public void reload ()
    {
        if (_main.page != null) {
            setMainPage(_main.page, _main.token);
        }
    }

    /**
     * Restarts the flash client. Normally only needed in response to the user being newly
     * registered.
     */
    public void rebootFlashClient ()
    {
        WorldClient.rebootFlash(_layout.getClientProvider());
    }

    protected void setMainPage (Pages page, String token)
    {
        // clear out any old content
        clearContent(page == Pages.WORLD);

        // make a note of our current page
        _main.page = page;
        _main.token = token;

        // show the header for pages that report a tab of which they are a part
        _header.selectTab(page.getTab());

        // if we're displaying a world page, that's special
        if (page == Pages.WORLD) {
            WorldClient.contentCleared();
            displayWorld(_main.token);

            // For facebook layouts where #world is the first page we visit, need to boot up the
            // title bar too
            if (_layout.alwaysShowsTitleBar()) {
                if (_bar == null) {
                    _bar = TitleBar.create(_layout, null, _closeContent);
                    _bar.setCloseVisible(true);
                }
                _layout.setTitleBar(_bar);
            }
            return;
        }

        // tell the flash client we're minimizing it
        WorldClient.setMinimized(true);

        // create our page frame
        _main.frame = new PageFrame(_main.page, FrameId.MAIN.name());

        // if we're on a headerless page or we only support one screen, we need to close the client
        if (isHeaderless() || _embedding.mode.isMonoscreen()) {
            closeClient();
        }

        if (isHeaderless() && !_layout.alwaysShowsTitleBar()) {
            _bar = null;

        } else {
            _bar = TitleBar.create(_layout, page.getTab(), _closeContent);
            _bar.setCloseVisible(FlashClients.clientExists());
        }

        _layout.setContent(_bar, _main.frame);
        _bottom.page = null;
        _bottom.token = "";
        _bottom.frame = null;

        // let the flash client know we are changing pages
        WorldClient.contentRequested(_main.page, _main.token);
    }

    protected void clearContent (boolean restoreClient)
    {
        if (_layout.hasContent()) {
            _layout.closeContent(restoreClient);

            // restore the title to the last thing flash asked for
            setTitle(_closeTitle, false);
        }

        // let the Flash client know that it's being unminimized or to start unminimized
        WorldClient.setMinimized(false);

        _main.frame = null;
        _main.page = null;
        _bottom.page = null;
        _bottom.token = "";
        _bottom.frame = null;
        if (!_layout.alwaysShowsTitleBar()) {
            _bar = null;
        }
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
            _listener.onClientClosed(didLogoff);
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
                                   Pages.WORLD.makeToken("s" + sceneId));
            }

        } else if (action.equals("game")) {
            // display a game lobby or enter a game (action_gameId_otherid1_token_otherid2)
            displayGame(args.get(1, ""), args.get(2, 0), args.get(3, 0), args.get(4, ""),
                        args.get(5, 0));

        } else if (action.equals("fbgame")) {
            // we're entering a chromeless facebook game (fbgame_gameId_fbid_fbtok)
            _listener.onEnterFacebookGame(args.get(2, ""), args.get(3, ""));
            FlashClients.setChromeless(true);
            displayGame("p", args.get(1, 0), 0, "", 0);

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

    /**
     * Displays a world client for viewing a scene.
     */
    protected void displayWorldClient (String args, String closeToken)
    {
        displayWorldClient(args, closeToken, null);
    }

    /**
     * Displays a world client for playing a game.
     */
    protected void displayWorldClient (String args, String closeToken, LaunchConfig game)
    {
        // note the current history token so that we can restore it if needed
        String currentToken = _main.page != null ?
            _main.page.makeToken(Args.fromToken(_main.token)) : null;
        _closeToken = (closeToken == null) ? currentToken : closeToken;

        // finally actually display the client
        WorldClient.displayFlash(args, _layout.getClientProvider());

        TitleBar bar = TitleBar.createClient(_layout, game);
        if (bar != null) {
            _bar = bar;
            _bar.setCloseVisible(!_embedding.mode.isMonoscreen());
            _layout.setTitleBar(_bar);
        }
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
        if (_embedding.mode.isFacebookGames()) {
            go(FrameId.BOTTOM, Pages.FACEBOOK, Args.compose("game", gameId).toToken());
        }
    }

    protected void launchGame (final LaunchConfig config, String action, final int otherId1,
                               String token, final int otherId2)
    {
        // configure our world client with a default host and port in case we're first to the party
        WorldClient.setDefaultServer(config.groupServer, config.groupPort);

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
            displayWorldClient(args, null, config);
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

            // everything else ("p" and "i" and legacy codes) means 'play now'
            } else if (otherId1 != 0) {
                args += "&playerId=" + otherId1;
            }
            displayWorldClient(args + hostPort, null, config);
            break;


        default:
            CShell.log("Requested to display unsupported game type " + config.type + ".");
            break;
        }
    }

    protected Frame toFrame (FrameId frameId)
    {
        switch (frameId) {
        case MAIN: return _main;
        case BOTTOM: return _bottom;
        default: throw new IllegalArgumentException();
        }
    }

    /**
     * Checks if the current web document resides in a frame.
     */
    protected native static boolean isFramed () /*-{
        return $wnd.top != $wnd;
    }-*/;

    protected ClickHandler _closeContent = new ClickHandler() {
        @Override public void onClick (ClickEvent event) {
            closeContent();
        }
    };

    protected static class Frame
    {
        public Pages page;
        public String token;
        public PageFrame frame;
    }

    protected Embedding _embedding;
    protected Listener _listener;

    // TODO: make an object with page, token, frame
    protected Frame _main = new Frame(), _bottom = new Frame();

    protected String _closeToken, _closeTitle;

    protected FrameHeader _header;
    protected Layout _layout;
    protected TitleBar _bar;

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final WebUserServiceAsync _usersvc = GWT.create(WebUserService.class);
}
