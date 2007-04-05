//
// $Id$

package client.game;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.web.client.DeploymentConfig;
import com.threerings.msoy.web.client.GameService;
import com.threerings.msoy.web.client.GameServiceAsync;
import com.threerings.msoy.web.data.LaunchConfig;

import client.shell.Page;
import client.shell.WorldClient;
import client.util.MsoyUI;

/**
 * Displays a page that allows a player to play a particular game. If it's
 * single player the game is shown, if it's multiplayer the lobby is first
 * shown where the player can find opponents against which to play.
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

    // @Override from Page
    public void onHistoryChanged (String token)
    {
        // if we're not a dev deployment, disallow guests
        if (!DeploymentConfig.devDeployment && CGame.creds == null) {
            setContent(MsoyUI.createLabel(CGame.cmsgs.noGuests(), "infoLabel"));
            return;
        }

        try {
            loadLaunchConfig(Integer.parseInt(token));
        } catch (Exception e) {
            // TODO: display error
        }
    }

    // @Override // from Page
    protected String getPageId ()
    {
        return "game";
    }

    // @Override // from Page
    protected void initContext ()
    {
        super.initContext();

        // wire up our remote services
        CGame.gamesvc = (GameServiceAsync)GWT.create(GameService.class);
        ((ServiceDefTarget)CGame.gamesvc).setServiceEntryPoint("/gamesvc");

        // load up our translation dictionaries
        CGame.msgs = (GameMessages)GWT.create(GameMessages.class);
    }

    protected void loadLaunchConfig (int gameId)
    {
        // load up the information needed to launch the game
        CGame.gamesvc.loadLaunchConfig(CGame.creds, gameId, new AsyncCallback() {
            public void onSuccess (Object result) {
                launchGame((LaunchConfig)result);
            }
            public void onFailure (Throwable cause) {
                CGame.serverError(cause);
            }
        });
    }

    protected void launchGame (LaunchConfig config)
    {
        Widget display = null;
        boolean contentIsFlash = false, contentIsJava = false;

        switch (config.type) {
        case LaunchConfig.FLASH_IN_WORLD:
            WorldClient.display("game", getPageArgs(), "worldGame=" + config.gameId);
            break;

        case LaunchConfig.FLASH_LOBBIED:
            WorldClient.display("game", getPageArgs(), "gameLobby=" + config.gameId);
            break;

        case LaunchConfig.FLASH_SOLO:
            display = WidgetUtil.createFlashContainer("game", config.gameMediaPath, 800, 600, null);
            contentIsFlash = true;
            break;

        case LaunchConfig.JAVA_LOBBIED:
            // TODO: need to nix the world client
            display = WidgetUtil.createApplet(
                "game", "/clients/" + DeploymentConfig.version + "/game-client.jar",
                "com.threerings.msoy.game.client.GameApplet", 800, 600,
                new String[] { "game_id", "" + config.gameId,
                               "resource_url", config.resourceURL,
                               "server", config.server,
                               "port", "" + config.port,
                               "authtoken", (CGame.creds == null) ? "" : CGame.creds.token });
            contentIsJava = true;
            break;

        case LaunchConfig.JAVA_SOLO:
            // TODO
            break;

        default:
            display = new Label(CGame.msgs.errUnknownGameType("" + config.type));
            break;
        }

        if (display != null) {
            setContent(display, contentIsFlash, contentIsJava);
        }
    }
}
