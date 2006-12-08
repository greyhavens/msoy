//
// $Id$

package client.util;

import com.google.gwt.user.client.ui.HTML;

import com.threerings.gwt.ui.WidgetUtil;

/**
 * Utility methods for generating flash clients.
 */
public class FlashClients
{
    public static HTML createWorldClient (String flashVars)
    {
        return WidgetUtil.createFlashContainer(
            "asclient", "/clients/game-client.swf", "100%", "550",
            flashVars);
    }

    public static HTML createChatClient ()
    {
        return WidgetUtil.createFlashContainer(
            "chat", "/clients/game-client.swf", "800", "150",
            "noplace=t");
    }

    public static HTML createLobbyClient (int gameId)
    {
        return WidgetUtil.createFlashContainer(
            "aslobby", "/clients/game-client.swf", "800", "600",
            "gameLobby=" + gameId);
    }
}
