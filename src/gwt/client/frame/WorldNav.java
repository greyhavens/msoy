//
// $Id$

package client.frame;

import com.google.gwt.core.client.GWT;

import com.threerings.gwt.util.StringUtil;
import com.threerings.msoy.data.all.LaunchConfig;
import com.threerings.msoy.web.gwt.Args;
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
         * Called when the token we are going to is "fbgame" and provides a user id and session for
         * Facebook interaction within the game.
         * @param uid the Facebook user id
         * @param session the Facebook session key
         */
        void onEnterFacebookGame (String uid, String session);

        /**
         * Called whenever we have just displayed a client.
         */
        void onClientDisplayed ();
    }

    /**
     * Creates a new world navigator.
     * @param provider the panel provider we use to embed the flash movie
     * @param listener receives basic feedback about our navigation
     */
    public WorldNav (WorldClient.PanelProvider provider, Listener listener)
    {
        _provider = provider;
        _listener = listener;
        Session.addObserver(new Session.Observer() {
            @Override public void didLogon (SessionData data) {
                // update the world client to relogin (this will NOOP if we're logging in now
                // because Flash just told us to do so)
                WorldClient.didLogon(data.creds);
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
        WorldClient.setMinimized(minimized);
    }

    /**
     * Go to the home room of whatever user is currently authenticated.
     */
    public void goHome ()
    {
        go(Args.compose("h").toToken());
    }

    /**
     * Reloads the widget containing the flash client so it restarts.
     */
    public void reload ()
    {
        WorldClient.rebootFlash(_provider);
    }

    /**
     * Notifies the flash client that we are about to load up new content.
     * @param page the page (module) of the content
     * @param token the arguments to the page
     */
    public void contentChanged (Pages page, String token)
    {
        WorldClient.contentRequested(page, token);
    }

    /**
     * Notifies the flash client that the previously requested content is now being viewed.
     * @param page the page (module) of the content
     * @param token the arguments to the page
     */
    public void contentReady (Pages page, String token)
    {
        WorldClient.contentPageReady(page, token);
    }

    /**
     * Notifies the flash client that the content was just closed.
     */
    public void contentCleared ()
    {
        WorldClient.contentCleared();
    }

    /**
     * Retrives the token we last navigated to. This does not always correspond to the actual
     * location of the flash ClientObject, due to bugs or unimplemented functionality for things
     * like AVRGs and parties.
     */
    public String getToken ()
    {
        return Pages.WORLD.makeToken(_args);
    }

    /**
     * Notifies the client that it is about to get closed and clears our location state.
     */
    public void willClose ()
    {
        WorldClient.clientWillClose();
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
            _listener.onEnterFacebookGame(_args.get(2, ""), _args.get(3, ""));
            FlashClients.setChromeless(true);
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

        // finally actually display the client
        WorldClient.displayFlash(args, _provider);

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
        WorldClient.setDefaultServer(config.groupServer, config.groupPort);

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

    protected WorldClient.PanelProvider _provider;
    protected Listener _listener;
    protected Args _args;
    protected String _title;
    protected LaunchConfig _game;

    protected static final WebUserServiceAsync _usersvc = GWT.create(WebUserService.class);
}
