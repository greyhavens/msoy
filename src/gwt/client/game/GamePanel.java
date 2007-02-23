//
// $Id$

package client.game;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.web.data.LaunchConfig;

import client.util.FlashClients;

/**
 * Displays the client interface for a particular game.
 */
public class GamePanel extends VerticalPanel
{
    public GamePanel (LaunchConfig config)
    {
        String authtoken = (CGame.creds == null) ? "" : CGame.creds.token;
        Widget display = null;
        switch (config.type) {
        case LaunchConfig.FLASH_IN_WORLD: {
            setWidth("100%");
            display = FlashClients.createWorldClient("worldGame=" + config.gameId);
            break;
        }

        case LaunchConfig.FLASH_LOBBIED: {
            display = FlashClients.createLobbyClient(config.gameId, authtoken);
            break;
        }

        case LaunchConfig.FLASH_SOLO: {
            display = WidgetUtil.createFlashContainer("game", config.gameMediaPath, 800, 600, null);
            break;
        }

        case LaunchConfig.JAVA_LOBBIED: {
            display = WidgetUtil.createApplet(
                "game", "/clients/game-client.jar",
                "com.threerings.msoy.game.client.GameApplet", 800, 600,
                new String[] { "game_id", "" + config.gameId,
                               "resource_url", config.resourceURL,
                               "server", config.server,
                               "port", "" + config.port,
                               "authtoken", authtoken });
            break;
        }

        case LaunchConfig.JAVA_SOLO: {
            // TODO
            break;
        }

        default:
            add(new Label(CGame.msgs.errUnknownGameType("" + config.type)));
            break;
        }

        if (display != null) {
            add(display);
        }
    }
}
