//
// $Id$

package client.game;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.client.WebContext;
import com.threerings.msoy.web.data.LaunchConfig;

import client.util.WidgetUtil;

/**
 * Displays the client interface for a particular game.
 */
public class GamePanel extends VerticalPanel
{
    public GamePanel (WebContext ctx, LaunchConfig config)
    {
        add(new Label(config.name));

        String authtoken = (ctx.creds == null) ? "" : ctx.creds.token;
        Widget display = null;
        switch (config.type) {
        case LaunchConfig.FLASH_LOBBIED: {
            display = WidgetUtil.createFlashMovie(
               // TODO: separate SWF for the lobby client
               "aslobby", "/clients/game-client.swf", "800", "600",
               "gameLobby=" + config.gameId);
            break;
        }

        case LaunchConfig.FLASH_SOLO: {
            display = WidgetUtil.createFlashMovie(
                "game", config.gameMediaPath, 800, 600, null);
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
            add(new Label("Zoiks! Uknown game type " + config.type + "."));
            break;
        }

        if (display != null) {
            add(display);
        }
    }
}
