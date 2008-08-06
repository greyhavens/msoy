//
// $Id$

package client.world;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.web.data.LaunchConfig;
import com.threerings.msoy.world.gwt.WorldService;
import com.threerings.msoy.world.gwt.WorldServiceAsync;

import client.shell.Args;
import client.shell.CShell;
import client.shell.Page;
import client.shell.Pages;
import client.ui.MsoyUI;
import client.util.FlashClients;
import client.util.MsoyCallback;
import client.util.ServiceUtil;
import client.util.events.GotGuestIdEvent;

/**
 * Handles the MetaSOY main page.
 */
public class WorldPage extends Page
{
    /** Required to map this entry point to a page. */
    public static Creator getCreator ()
    {
        return new Creator() {
            public Page createPage () {
                return new WorldPage();
            }
        };
    }

    @Override // from Page
    public void onHistoryChanged (Args args)
    {
        try {
            String action = args.get(0, "s1");
            if (action.startsWith("s")) {
                String sceneId = action.substring(1);
                if (args.getArgCount() <= 1) {
                    CWorld.frame.displayWorldClient("sceneId=" + sceneId, null);
                } else {
                    // if we have sNN-extra-args we want the close button to use just "sNN"
                    CWorld.frame.displayWorldClient(
                        "sceneId=" + sceneId + "&page=" + Args.compose(args.splice(1)),
                        Pages.WORLD + "-s" + sceneId);
                }

            } else if (action.equals("game")) {
                // display a game lobby or enter a game (action_gameId_gameOid)
                displayGame(args.get(1, ""), args.get(2, 0), args.get(3, -1));

            } else if (action.equals("room")) {
                setContent(new RoomPanel(args.get(1, 0)));

            } else if (action.startsWith("g")) {
                // go to a specific group's scene group
                CWorld.frame.displayWorldClient("groupHome=" + action.substring(1), null);

            } else if (action.startsWith("m")) {
                // go to a specific member's home
                CWorld.frame.displayWorldClient("memberHome=" + action.substring(1), null);

            } else if (action.startsWith("c")) {
                // join a group chat
                CWorld.frame.displayWorldClient("groupChat=" + action.substring(1), null);

            } else if (action.startsWith("p")) {
                // display popular places
                displayHotSpots();

            } else if (CWorld.isGuest()) {
                setContent(MsoyUI.createLabel(_msgs.logonForHome(), "infoLabel"));

            } else if (action.startsWith("h")) {
                // go to our home
                CWorld.frame.displayWorldClient("memberHome=" + CWorld.getMemberId(), null);

            } else {
                setContent(MsoyUI.createLabel(_msgs.unknownLocation(), "infoLabel"));
            }

        } catch (NumberFormatException e) {
            MsoyUI.error(_msgs.unknownLocation());
        }
    }

    @Override // from Page
    public void onPageLoad ()
    {
        super.onPageLoad();
        configureCallbacks(this);
    }

    @Override // from Page
    public void onPageUnload ()
    {
        super.onPageUnload();

        clearCallbacks();
    }

    @Override
    public Pages getPageId ()
    {
        return Pages.WORLD;
    }

    protected void displayHotSpots ()
    {
        _worldsvc.serializePopularPlaces(CWorld.ident, 20, new MsoyCallback<String>() {
            public void onSuccess (String result) {
                setFlashContent(null, FlashClients.createPopularPlacesDefinition(result));
            }
        });
    }

    protected void displayGame (final String action, int gameId, final int gameOid)
    {
        // load up the information needed to launch the game
        _worldsvc.loadLaunchConfig(CWorld.ident, gameId, new MsoyCallback<LaunchConfig>() {
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

        switch (config.type) {
        case LaunchConfig.FLASH_IN_WORLD:
            CWorld.frame.displayWorldClient("worldGame=" + config.gameId, null);
            break;

        case LaunchConfig.FLASH_LOBBIED:
            if (gameOid <= 0) {
                String hostPort = "&ghost=" + config.server + "&gport=" + config.port;
                if (action.equals("m") || action.equals("f") || action.equals("s")) {
                    CWorld.frame.displayWorldClient(
                        "playNow=" + config.gameId + "&mode=" + action + hostPort, null);
                } else {
                    CWorld.frame.displayWorldClient("gameLobby=" + config.gameId + hostPort, null);
                }
            } else {
                CWorld.frame.displayWorldClient("gameLocation=" + gameOid, null);
            }
            break;

        case LaunchConfig.JAVA_FLASH_LOBBIED:
        case LaunchConfig.JAVA_SELF_LOBBIED:
            if (config.type == LaunchConfig.JAVA_FLASH_LOBBIED && gameOid <= 0) {
                CWorld.frame.displayWorldClient("gameLobby=" + config.gameId, null);

            } else {
                // clear out the client as we're going into Java land
                CWorld.frame.closeClient();

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
                bits.add(WidgetUtil.createApplet("game", config.getURL(hpath),
                                                 "com.threerings.msoy.client.HowdyPardner",
                                                 "100", "10", true, new String[0]));
                setContent(bits);
            }
            break;

        case LaunchConfig.FLASH_SOLO:
            setFlashContent(
                    config.name, FlashClients.createSoloGameDefinition(config.gameMediaPath));
            break;

        case LaunchConfig.JAVA_SOLO:
            setContent(config.name, new Label("Not yet supported"));
            break;

        default:
            setContent(config.name, new Label(_msgs.errUnknownGameType("" + config.type)));
            break;
        }
    }

    protected void displayJava (LaunchConfig config, int gameOid)
    {
// TODO: all this information needs to be passed up to the Frame, so maybe the frame should just
// take care of all of this...
//         String[] args = new String[] {
//             "game_id", "" + config.gameId, "game_oid", "" + gameOid,
//             "server", config.server, "port", "" + config.port,
//             "authtoken", (CWorld.ident == null) ? "" : CWorld.ident.token };
//         String gjpath = "/clients/" + DeploymentConfig.version + "/" +
//             (config.lwjgl ? "lwjgl-" : "") + "game-client.jar";
//         WorldClient.displayJava(
//             WidgetUtil.createApplet(
//                 // here we explicitly talk directly to our game server (not via the public facing
//                 // URL which is a virtual IP) so that Java's security policy works
//                 "game", config.getURL(gjpath) + "," + config.getURL(config.gameMediaPath),
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

    protected static native void configureCallbacks (WorldPage page) /*-{
       $wnd.howdyPardner = function () {
            page.@client.world.WorldPage::javaReady()();
       }
    }-*/;

    protected static native void clearCallbacks () /*-{
       $wnd.howdyPardner = null;
    }-*/;

    /** A command to be run when Java reports readiness. */
    protected Command _javaReadyCommand;

    protected static final WorldMessages _msgs = GWT.create(WorldMessages.class);
    protected static final WorldServiceAsync _worldsvc = (WorldServiceAsync)
        ServiceUtil.bind(GWT.create(WorldService.class), WorldService.ENTRY_POINT);

    protected static final int NEIGHBORHOOD_REFRESH_TIME = 60;
}
