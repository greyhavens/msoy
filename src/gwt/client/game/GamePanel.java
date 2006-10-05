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

        Widget display = null;
        switch (config.type) {
        case LaunchConfig.FLASH_LOBBIED: {
            // TODO
            break;
        }

        case LaunchConfig.FLASH_SOLO: {
            display = WidgetUtil.createFlashMovie(
                "game", config.gameMediaURL, 800, 600);
            break;
        }

        case LaunchConfig.JAVA_LOBBIED: {
            display = WidgetUtil.createApplet(
                "game", "/clients/game-client.jar",
                "com.threerings.msoy.game.client.GameApplet", 800, 600,
                new String[] { "game_id", "" + config.gameId,
                               "resource_url",
                               "http://" + config.server + "/media/", // TODO
                               "server", config.server,
                               "port", "" + config.port });
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
