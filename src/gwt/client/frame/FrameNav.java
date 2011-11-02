//
// $Id$

package client.frame;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Panel;

import com.threerings.gwt.util.StringUtil;

import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Embedding;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.SessionData;

import client.shell.CShell;
import client.shell.Session;
import client.shell.ShellMessages;
import client.util.FlashClients;
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

        // flash callbacks
        configureCallbacks();

        // listen for logon/logoff
        Session.addObserver(new Session.Observer() {
            @Override public void didLogon (SessionData data) {
                // now that we know we're a member, we can add our "open home in minimized mode"
                // icon (which may get immediately removed if we're going directly into the world)
                if (CShell.getClientMode().isMinimal()) {
                    if (_bar != null) {
                        _bar.setCloseVisible(true);
                    }
                    _world.goHome();
                } else if (!isHeaderless()) {
                    _layout.addNoClientIcon(data);
                }
            }
            @Override public void didLogoff () {
                // close the Flash client if it's open
                closeClient(true);
            }
        });

        // create our header
        _header = new FrameHeader(new ClickHandler() {
            public void onClick (ClickEvent event) {
                if (_world.getToken() != null) {
                    closeContent();
                } else {
                    _listener.onLogoClick();
                }
            }
        });

        // create our frame layout
        _layout = Layout.create(_header, embedding.mode, isFramed(), new ClickHandler() {
            public void onClick (ClickEvent event) {
                // show the close box in the title bar so content can be dismissed
                if (_bar != null) {
                    _bar.setCloseVisible(true);
                }

                // minimize and go home
                _world.setMinimized(true);
                _world.goHome();
            }
        });

        _world = new WorldNav(new WorldNav.PanelProvider() {
            @Override public Panel get () {
                return _layout.prepareClientPanel();
            }
        }, new WorldNav.Listener() {
            @Override public void onClientDisplayed () {
                TitleBar bar = TitleBar.createClient(_layout, _world.getGame());
                if (bar != null) {
                    _bar = bar;
                    _bar.setCloseVisible(!_embedding.mode.isMonoscreen());
                    _layout.setTitleBar(_bar);
                }

                if (_world.getGame() != null && _embedding.mode.isFacebookGames()) {
                    Args args = Args.compose("game", _world.getGame().gameId);
                    go(FrameId.BOTTOM, Pages.FACEBOOK, args.toToken());
                }
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
        // de-nullify
        token = StringUtil.getOr(token, "");

        // bottom heuristics are simpler (cannot visit funky /#world there)
        if (frame == FrameId.BOTTOM) {
            if (_bottom.set(page, token)) {
                _layout.setBottomContent(_bottom.createFrame());
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
            _world.contentChanged(_main.page, _main.token);
        }
    }

    /**
     * Notifies the navigator that the previously requested content is now being displayed. This
     * is required because the module callbacks are declared elsewhere.
     * @param frame the frame that is displaying the content
     * @param page the page (module) being displayed
     * @param token the page argument that was processed
     */
    public void contentSet (FrameId frame, Pages page, String token)
    {
        if (frame != FrameId.MAIN) {
            return;
        }

        // let the world know we're now looking at this content, but only if it is the
        // right content
        if (_main.page == page && _main.token.equals(token)) {
            _world.contentReady(_main.page, _main.token);
        }
    }

    /**
     * Sets the title of the window and, if appropriate, the title bar. This is normally called
     * when a module if finally loaded via the frame module's page callbacks.
     * @param title the title to set
     */
    public void setTitle (String title)
    {
        setTitle(title, false);
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
        String token = _world.getToken();
        if (token != null) {
            _listener.onContentClosed(token);
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
        _world.reload();
    }

    protected void setMainPage (Pages page, String token)
    {
        // clear out any old content
        clearContent(page == Pages.WORLD);

        // make a note of our current page
        _main.set(page, token);

        // show the header for pages that report a tab of which they are a part
        _header.selectTab(page.getTab());

        // if we're displaying a world page, that's special
        if (page == Pages.WORLD) {
            // delegate most of the work to the world nav
            _world.contentCleared();
            _world.go(_main.token);

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

        _layout.setContent(_bar, _main.createFrame());

        // tell the flash client we're minimizing it
        _world.setMinimized(true);

        // let the flash client know we are changing pages
        _world.contentChanged(_main.page, _main.token);
    }

    protected void clearContent (boolean restoreClient)
    {
        if (_layout.hasContent()) {
            _layout.closeContent(restoreClient);

            // restore the title to the last thing flash asked for
            setTitle(_world.getTitle());
        }

        // let the Flash client know that it's being unminimized or to start unminimized
        _world.setMinimized(false);

        _main.clear();
        _bottom.clear();
        if (!_layout.alwaysShowsTitleBar()) {
            _bar = null;
        }
    }

    protected void closeClient (boolean didLogoff)
    {
        _world.willClose();

        if (_bar != null) {
            _bar.setCloseVisible(false);
        }

        if (_layout.closeClient()) {
            _listener.onClientClosed(didLogoff);
        }
    }

    protected FrameInfo toFrame (FrameId frameId)
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

    protected void setTitle (String title, boolean fromFlash)
    {
        // if we're displaying content currently, don't let flash mess with the title
        if (!fromFlash || !_layout.hasContent()) {
            Window.setTitle(title == null ? _cmsgs.bareTitle() : _cmsgs.windowTitle(title));
            if (title != null && _bar != null) {
                _bar.setTitle(title);
            }
        }
        if (fromFlash) {
            _world.setTitle(title);
        }
    }

    protected void setTitleFromFlash (String title)
    {
        setTitle(title, true);
    }

    protected void deferredCloseClient ()
    {
        DeferredCommand.addCommand(new Command() {
            public void execute () {
                closeClient();
            }
        });
    }

    protected native void configureCallbacks () /*-{
        var fnav = this;
        $wnd.setWindowTitle = function (title) {
            fnav.@client.frame.FrameNav::setTitleFromFlash(Ljava/lang/String;)(title);
        };
        $wnd.clearClient = function () {
            fnav.@client.frame.FrameNav::deferredCloseClient()();
        };
    }-*/;

    protected ClickHandler _closeContent = new ClickHandler() {
        @Override public void onClick (ClickEvent event) {
            closeContent();
        }
    };

    protected static class FrameInfo
    {
        public FrameId frameId;
        public Pages page;
        public String token = "";
        public PageFrame frame;

        public FrameInfo (FrameId id)
        {
            frameId = id;
        }

        public PageFrame createFrame ()
        {
            return frame = new PageFrame(page, frameId.name());
        }

        public boolean set (Pages page, String token)
        {
            if (this.page == page && this.token.equals(token)) {
                return false;
            }

            this.page = page;
            this.token = token;
            this.frame = null;
            return true;
        }

        public void clear ()
        {
            set(null, "");
        }
    }

    protected Embedding _embedding;
    protected Listener _listener;
    protected FrameInfo _main = new FrameInfo(FrameId.MAIN);
    protected FrameInfo _bottom = new FrameInfo(FrameId.BOTTOM);
    protected WorldNav _world;
    protected FrameHeader _header;
    protected Layout _layout;
    protected TitleBar _bar;

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
}
