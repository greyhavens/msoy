//
// $Id$

package client.world;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MouseListenerAdapter;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.web.client.DeploymentConfig;
import com.threerings.msoy.web.client.WorldService;
import com.threerings.msoy.web.client.WorldServiceAsync;
import com.threerings.msoy.web.data.LaunchConfig;

import client.shell.Application;
import client.shell.Args;
import client.shell.Frame;
import client.shell.Page;
import client.shell.WorldClient;
import client.util.FlashClients;
import client.util.MsoyUI;

/**
 * Handles the MetaSOY main page.
 */
public class index extends Page
{
    /** Required to map this entry point to a page. */
    public static Creator getCreator ()
    {
        return new Creator() {
            public Page createPage () {
                return new index();
            }
        };
    }

    // @Override // from Page
    public void onHistoryChanged (Args args)
    {
        _entryCounter++;

        // cancel our refresher interval as we'll restart it if needed below
        if (_refresher != null) {
            _refresher.cancel();
            _refresher = null;
        }

        // don't show the flash client in the GWT shell
        if (!GWT.isScript()) {
            return;
        }

        // if we're not a dev deployment, disallow guests
        if (!DeploymentConfig.devDeployment && CWorld.ident == null) {
            setContent(MsoyUI.createLabel(CWorld.cmsgs.noGuests(), "infoLabel"));
            return;
        }

        try {
            String action = args.get(0, "s1");
            if (action.startsWith("s")) {
                String sceneId = action.substring(1);
                if (args.getArgCount() <= 1) {
                    WorldClient.displayFlash("sceneId=" + sceneId);
                } else {
                    // if we have sNN-extra-args we want the close button to use just "sNN"
                    WorldClient.displayFlash("sceneId=" + sceneId + "&page=" +
                                             Args.compose(args.splice(1)), "s" + sceneId);
                }

            } else if (action.equals("game")) {
                // display a game lobby or enter a game (action_gameId_gameOid)
                displayGame(args.get(1, ""), args.get(2, 0), args.get(3, -1));

            } else if (action.startsWith("g")) {
                // go to a specific group's scene group
                WorldClient.displayFlash("groupHome=" + action.substring(1));

            } else if (action.startsWith("m")) {
                // go to a specific member's home
                WorldClient.displayFlash("memberHome=" + action.substring(1));

            } else if (action.startsWith("c")) {
                // join a group chat
                WorldClient.displayFlash("groupChat=" + action.substring(1));

            } else if (action.startsWith("p")) {
                // display popular places by request
                displayHotSpots(_entryCounter);

            } else {
                MsoyUI.error(CWorld.msgs.unknownLocation());
            }

        } catch (NumberFormatException e) {
            MsoyUI.error(CWorld.msgs.unknownLocation());
        }
    }

    // @Override // from Page
    public void onPageLoad ()
    {
        super.onPageLoad();
        configureCallbacks(this);
    }

    // @Override // from Page
    public void onPageUnload ()
    {
        super.onPageUnload();

        clearCallbacks();

        if (_refresher != null) {
            _refresher.cancel();
            _refresher = null;
        }
    }

    // @Override // from Page
    protected void didLogoff ()
    {
        // head to Whirledwide
        Application.go(Page.WHIRLED, "whirledwide");
    }

    // @Override // from Page
    protected void initContext ()
    {
        super.initContext();

        // wire up our remote services
        CWorld.worldsvc = (WorldServiceAsync)GWT.create(WorldService.class);
        ((ServiceDefTarget)CWorld.worldsvc).setServiceEntryPoint("/worldsvc");

        // load up our translation dictionaries
        CWorld.msgs = (WorldMessages)GWT.create(WorldMessages.class);
    }

    // @Override // from Page
    protected String getPageId ()
    {
        return WORLD;
    }

    protected void displayHotSpots (final int requestEntryCount)
    {
        CWorld.worldsvc.serializePopularPlaces(CWorld.ident, 20, new AsyncCallback() {
            public void onSuccess (Object result) {
                if (requestEntryCount == _entryCounter) {
                    setPageTitle(CWorld.msgs.hotSpotsTitle());
                    setFlashContent(FlashClients.createPopularPlacesDefinition((String) result));
                }
            }
            public void onFailure (Throwable caught) {
                if (requestEntryCount == _entryCounter) {
                    setContent(new Label(CWorld.serverError(caught)));
                }
            }
        });
    }

    protected void displayGame (final String action, int gameId, final int gameOid)
    {
        // load up the information needed to launch the game
        CWorld.worldsvc.loadLaunchConfig(CWorld.ident, gameId, new AsyncCallback() {
            public void onSuccess (Object result) {
                launchGame((LaunchConfig)result, gameOid, action);
            }
            public void onFailure (Throwable cause) {
                MsoyUI.error(CWorld.serverError(cause));
            }
        });
    }

    protected void launchGame (final LaunchConfig config, final int gameOid, String action)
    {
        switch (config.type) {
        case LaunchConfig.FLASH_IN_WORLD:
            WorldClient.displayFlash("worldGame=" + config.gameId);
            break;

        case LaunchConfig.FLASH_LOBBIED:
            if (gameOid <= 0) {
                if (action.equals("m")) {
                    WorldClient.displayFlash("playNow=" + config.gameId + "&single=false");
                } else if (action.equals("s")) {
                    WorldClient.displayFlash("playNow=" + config.gameId + "&single=true");
                } else {
                    WorldClient.displayFlash("gameLobby=" + config.gameId);
                }
            } else {
                WorldClient.displayFlash("gameLocation=" + gameOid);
            }
            break;

        case LaunchConfig.JAVA_FLASH_LOBBIED:
        case LaunchConfig.JAVA_SELF_LOBBIED:
            if (config.type == LaunchConfig.JAVA_FLASH_LOBBIED && gameOid <= 0) {
                WorldClient.displayFlash("gameLobby=" + config.gameId);

            } else {
                // clear out the client as we're going into Java land
                Frame.closeClient(false);

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

                String gameServer = "http://" + config.server + ":" + config.httpPort;
                String howdyJar =
                    gameServer + "/clients/" + DeploymentConfig.version + "/howdy.jar";
                bits.add(WidgetUtil.createApplet("game", howdyJar,
                                                 "com.threerings.msoy.client.HowdyPardner",
                                                 "100", "10", true, new String[0]));
                setContent(bits);
            }
            break;

        case LaunchConfig.FLASH_SOLO:
            setPageTitle(config.name);
            setFlashContent(WidgetUtil.createFlashObjectDefinition(
                                "game", config.gameMediaPath, 800, 600, null));
            break;

        case LaunchConfig.JAVA_SOLO:
            setPageTitle(config.name);
            setContent(new Label("Not yet supported"));
            break;

        default:
            setPageTitle(config.name);
            setContent(new Label(CWorld.msgs.errUnknownGameType("" + config.type)));
            break;
        }
    }

    protected void displayJava (LaunchConfig config, int gameOid)
    {
        String[] args = new String[] {
            "game_id", "" + config.gameId, "game_oid", "" + gameOid,
            "server", config.server, "port", "" + config.port,
            "authtoken", (CWorld.ident == null) ? "" : CWorld.ident.token };

        // we have to serve game-client.jar from the server to which it will connect back due to
        // security restrictions and proxy the game jar through there as well
        String gameServer = "http://" + config.server + ":" + config.httpPort;
        String gameJar = gameServer + "/clients/" +
            DeploymentConfig.version + "/" + (config.lwjgl ? "lwjgl-" : "") +
            "game-client.jar";

        WorldClient.displayJava(
            WidgetUtil.createApplet(
                "game", gameJar + "," + gameServer + config.gameMediaPath,
                // TODO: allow games to specify their dimensions in their config
                "com.threerings.msoy.game.client." +
                (config.lwjgl ? "LWJGL" : "") + "GameApplet",
                "100%", "600", false, args));
    }

    protected void javaReady ()
    {
        if (_javaReadyCommand != null) {
            DeferredCommand.addCommand(_javaReadyCommand);
            _javaReadyCommand = null;
        }
    }

    protected static native void configureCallbacks (index page) /*-{
       $wnd.howdyPardner = function () {
            page.@client.world.index::javaReady()();
       }
    }-*/;

    protected static native void clearCallbacks () /*-{
       $wnd.howdyPardner = null;
    }-*/;

    /** A counter to help asynchronous callbacks to figure out if they've been obsoleted. */
    protected int _entryCounter;

    /** Handles periodic refresh of the popular places view. */
    protected Timer _refresher;

    /** A command to be run when Java reports readiness. */
    protected Command _javaReadyCommand;

    protected static final int NEIGHBORHOOD_REFRESH_TIME = 60;
}
