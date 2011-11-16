//
// $Id$

package client.frame;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Panel;

import com.threerings.gwt.util.CookieUtil;
import com.threerings.gwt.util.StringUtil;

import com.threerings.msoy.data.all.LaunchConfig;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.ClientMode;
import com.threerings.msoy.web.gwt.ConnectConfig;
import com.threerings.msoy.web.gwt.CookieNames;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.SessionData;
import com.threerings.msoy.web.gwt.WebUserService;
import com.threerings.msoy.web.gwt.WebUserServiceAsync;

import client.shell.CShell;
import client.shell.Session;
import client.util.FlashClients;
import client.util.InfoCallback;

/**
 * Handles all navigation within the msoy world client. Note that this is effectively a singleton
 * since requests are normally dispatched to a specific document element.
 * TODO: investigate combining this with class WorldClient
 */
public class WorldNav
{
    /**
     * Provides very basic feedback on what's happening in the world.
     */
    public interface Listener
    {
        /**
         * Called whenever we have just displayed a client.
         */
        void onClientDisplayed ();
    }

    /**
     * Provides the panel for the world client.
     */
    public interface PanelProvider
    {
        /**
         * Prepares, creates or gets a panel to embed the world client in.
         */
        Panel get ();
    }

    /**
     * Creates a new world navigator.
     * @param provider the panel provider we use to embed the flash movie
     * @param listener receives basic feedback about our navigation
     */
    public WorldNav (PanelProvider provider, Listener listener)
    {
        _provider = provider;
        _listener = listener;

        configureCallbacks();

        Session.addObserver(new Session.Observer() {
            @Override public void didLogon (SessionData data) {
                // update the world client to relogin (this will NOOP if we're logging in now
                // because Flash just told us to do so)
                if (_flashPanel != null) {
                    nativeLogon(findClient(), data.creds.getMemberId(), data.creds.token);
                }
            }

            @Override public void didLogoff () {
            }
        });
    }

    /**
     * Directs the world client to the given token.
     * @param token the token to go to, e.g. "s12" or "game_p_17"
     */
    public void go (String token)
    {
        // note the current history token so that we can restore it if needed
        _args = Args.fromToken(token);
        display();
    }

    /**
     * Sets the internal title of the current flash scene or game. This is needed because the flash
     * callbacks are defined elsewhere. The title is retrieved later in order to set the window
     * title after the content area is close.
     * @param title the tile of the flash scene or game
     */
    public void setTitle (String title)
    {
        _title = title;
    }

    /**
     * Retrieves the most recently set title of the flash scene or game.
     */
    public String getTitle ()
    {
        return _title;
    }

    /**
     * Returns the game last navigated to via gwt, or null if the last navigation was to a scene.
     */
    public LaunchConfig getGame ()
    {
        return _game;
    }

    /**
     * Lets the world client know that it is minimized.
     */
    public void setMinimized (boolean minimized)
    {
        _minimized = minimized;
        nativeMinimized(findClient(), minimized);
    }

    /**
     * Go to the home room of whatever user is currently authenticated.
     */
    public void goHome ()
    {
        go(Args.compose("h").toToken());
    }

    /**
     * Open flash and go home, unless the player is already connected in another tab.
     */
    public void goAutoConnect ()
    {
        go(Args.compose("a").toToken());
    }

    /**
     * Reloads the widget containing the flash client so it restarts.
     */
    public void reload ()
    {
        displayClient(_flashArgs, true);
    }

    /**
     * Notifies the flash client that we are about to load up new content.
     * @param page the page (module) of the content
     * @param token the arguments to the page
     */
    public void contentChanged (Pages page, String token)
    {
        // no need to pass this along right now
    }

    /**
     * Notifies the flash client that the previously requested content is now being viewed.
     * @param page the page (module) of the content
     * @param token the arguments to the page
     */
    public void contentReady (Pages page, String token)
    {
        nativeSetPage(findClient(), page.name(), token);
    }

    /**
     * Notifies the flash client that the content was just closed.
     */
    public void contentCleared ()
    {
        nativeSetPage(findClient(), null, null);
    }

    /**
     * Retrives the token we last navigated to. This does not always correspond to the actual
     * location of the flash ClientObject, due to bugs or unimplemented functionality for things
     * like AVRGs and parties.
     */
    public String getToken ()
    {
        return _args == null ? null : Pages.WORLD.makeToken(_args);
    }

    /**
     * Notifies the client that it is about to get closed and clears our location state.
     */
    public void willClose ()
    {
        unload();
        _args = null;
        _title = null;
    }

    protected void display ()
    {
        String action = _args.get(0, "");
        if (action.startsWith("s")) {
            String sceneId = action.substring(1);
            if (_args.getArgCount() <= 1) {
                display("sceneId=" + sceneId);
            } else {
                // if we have sNN-extra-args we want the close button to use just "sNN"
                _args = Args.compose("s", sceneId);
                display("sceneId=" + sceneId + "&page=" + _args.get(1, "") +
                                   "&args=" + _args.recompose(2));
            }

        } else if (action.equals("game")) {
            // display a game lobby or enter a game (action_gameId_otherid1_token_otherid2)
            loadAndDisplayGame(_args.get(1, ""), _args.get(2, 0), _args.get(3, 0),
                               _args.get(4, ""), _args.get(5, 0));

        } else if (action.equals("fbgame")) {
            // we're entering a chromeless facebook game (fbgame_gameId_fbid_fbtok)
            _facebookId = _args.get(2, "");
            _facebookSession = _args.get(3, "");
            _chromeless = true;
            loadAndDisplayGame("p", _args.get(1, 0), 0, "", 0);

        } else if (action.equals("tour")) {
            display("tour=true");

        } else if (action.startsWith("g")) {
            // go to a specific group's scene group
            display("groupHome=" + action.substring(1));

        } else if (action.startsWith("m")) {
            // go to a specific member's home
            display("memberHome=" + action.substring(1));

        } else if (action.startsWith("c")) {
            // join a group chat
            display("groupChat=" + action.substring(1));

        } else if (action.equals("h")) {
            // go to our home
            display("memberHome=" + CShell.getMemberId());

        } else if (action.equals("a")) {
            // go to our home, maybe
            display("memberHome=" + CShell.getMemberId());
            display("auto=true");

        } else if (action.equals("hplaces")) {
            // just logon and show the myplaces dialog, don't go anywhere
            display("myplaces=true");

        } else { // (action == "places" or anything else)
            // just logon and go home for now
            display("memberHome=" + CShell.getMemberId());
        }
    }

    protected void display (String args)
    {
        display(args, null);
    }

    protected void display (String args, LaunchConfig game)
    {
        // store (or clear) the launch config
        _game = game;

        if (CShell.frame.getThemeId() != 0) {
            args += "&themeId=" + CShell.frame.getThemeId();
        }
        if (CShell.getClientMode() == ClientMode.DJ_WHIRLED && !CShell.creds.djTutorialComplete) {
            args += "&djTutorial=true";
        }

        // finally actually display the client
        displayClient(args, false);

        _listener.onClientDisplayed();
    }

    protected void loadAndDisplayGame (final String action, int gameId, final int otherId1,
                                       final String token, final int otherId2)
    {
        // load up the information needed to launch the game
        _usersvc.loadLaunchConfig(gameId, new InfoCallback<LaunchConfig>() {
            public void onSuccess (LaunchConfig result) {
                displayGame(result, action, otherId1, token, otherId2);
            }
        });
    }

    protected void displayGame (final LaunchConfig config, String action, final int otherId1,
                               String token, final int otherId2)
    {
        // configure our world client with a default host and port in case we're first to the party
        _defaultHost = config.groupServer;
        _defaultPort = config.groupPort;

        // sanitize our token
        token = StringUtil.getOr(token, "");

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
            display(args, config);
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
            display(args + hostPort, config);
            break;

        default:
            CShell.log("Requested to display unsupported game type " + config.type + ".");
            break;
        }
    }

    protected void displayClient (String flashArgs, final boolean forceRestart)
    {
        // if we have not yet determined our default server, find that out now
        if (_defaultHost == null) {
            final String savedArgs = flashArgs;
            _usersvc.getConnectConfig(new InfoCallback<ConnectConfig>() {
                public void onSuccess (ConnectConfig config) {
                    _defaultHost = config.server;
                    _defaultPort = config.port;
                    displayClient(savedArgs, forceRestart);
                }
            });
            return;
        }

        // if we're currently already displaying exactly what we've been asked to display; then
        // stop here because we're just restoring our client after closing a GWT page
        if (!forceRestart && flashArgs.equals(_flashArgs)) {
            return;
        }

        // create our client if necessary
        if (!forceRestart && _flashPanel != null && nativeGo(findClient(), flashArgs)) {
            _flashArgs = flashArgs; // note our new current flash args
            setMinimized(false); // TODO: why is this here?

        } else {
            // flash is not resolved or it's hosed, create or recreate the client
            unload(); // clear our clients if we have any

            _flashPanel = _provider.get();
            _flashArgs = flashArgs;

            // augment the arguments with things that are only relevant to the initial embed, i.e. not
            // logically part of the location of the client
            if (flashArgs.indexOf("&host") == -1) {
                flashArgs += "&host=" + _defaultHost;
            }
            if (flashArgs.indexOf("&port") == -1) {
                flashArgs += "&port=" + _defaultPort;
            }
            if (CShell.getAuthToken() != null) {
                flashArgs += "&token=" + CShell.getAuthToken();
            }
            if (_minimized) {
                flashArgs += "&minimized=t";
            }
            if (_chromeless) {
                flashArgs += "&chromeless=true";
            }
            String affstr = CookieUtil.get(CookieNames.AFFILIATE);
            if (!StringUtil.isBlank(affstr)) {
                flashArgs += "&aff=" + affstr;
            }

            _flashPanel.clear();
            FlashClients.embedWorldClient(_flashPanel, flashArgs);
        }
    }

    protected void unload ()
    {
        if (_flashPanel != null) {
            nativeUnload(findClient());
            _flashArgs = null;
            _flashPanel = null;
        }
    }

    protected String getFacebookId ()
    {
        return _facebookId;
    }

    protected String getFacebookSession ()
    {
        return _facebookSession;
    }

    protected void setPermaguestInfo (String name, String token)
    {
        // the server has created a permaguest account for us via flash, store the cookies
        CShell.log("Got permaguest info from flash", "name", name, "token", token);
        Session.conveyLoginFromFlash(token);
    }

    protected Element findClient ()
    {
        return FlashClients.findClient();
    }

    protected native void configureCallbacks () /*-{
        var wnav = this;
        // anoyingly these have to be specified separately, ActionScript chokes on String[]
        $wnd.getFacebookId = function () {
             return wnav.@client.frame.WorldNav::getFacebookId()();
        };
        $wnd.getFacebookSession = function () {
             return wnav.@client.frame.WorldNav::getFacebookSession()();
        };
        $wnd.rebootFlashClient = function () {
            wnav.@client.frame.WorldNav::reload()();
        }
        $wnd.setPermaguestInfo = function (name, token) {
            wnav.@client.frame.WorldNav::setPermaguestInfo(Ljava/lang/String;Ljava/lang/String;)(name, token);
        }
    }-*/;

    protected native boolean nativeGo (Element client, String where) /*-{
        if (client) {
            try { return client.clientGo(where); } catch (e) {}
        }
        return false;
    }-*/;

    protected static native void nativeLogon (Element client, int memberId, String token) /*-{
        if (client) {
            try { client.clientLogon(memberId, token); } catch (e) {}
        }
    }-*/;

    protected static native void nativeUnload (Element client) /*-{
        if (client) {
            try { client.onUnload(); } catch (e) {}
        }
    }-*/;

    protected static native void nativeMinimized (Element client, boolean mini) /*-{
        if (client) {
            try { client.setMinimized(mini); } catch (e) {}
        }
    }-*/;

    protected static native void nativeSetPage (Element client, String page, String token) /*-{
        if (client) {
            try { client.setPage(page, token); } catch (e) {}
        }
    }-*/;

    protected PanelProvider _provider;
    protected Listener _listener;
    protected Args _args;
    protected String _title;
    protected LaunchConfig _game;
    protected String _facebookId, _facebookSession;

    protected String _flashArgs;
    protected Panel  _flashPanel;
    protected boolean _minimized;

    /** Whether or not the client is in chromeless mode. */
    protected boolean _chromeless;

    /** Our default world server host and port. Configured the first time Flash is used. */
    protected String _defaultHost;
    protected int _defaultPort;

    protected static final WebUserServiceAsync _usersvc = GWT.create(WebUserService.class);
}
