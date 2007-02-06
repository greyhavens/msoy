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
    public static final String HOOD_SKIN_URL = "/media/static/hood_pastoral.swf";

    public static HTML createWorldClient (String flashVars)
    {
        return WidgetUtil.createFlashContainer(
            "asclient", "/clients/world-client.swf", "90%", "550",
            flashVars);
    }

    public static HTML createChatClient ()
    {
        return WidgetUtil.createFlashContainer(
            "chat", "/clients/world-client.swf", "800", "150",
            "noplace=t");
    }

    public static HTML createLobbyClient (int gameId)
    {
        return WidgetUtil.createFlashContainer(
            "aslobby", "/clients/world-client.swf", "800", "600",
            "gameLobby=" + gameId);
    }

    public static HTML createNeighborhood (String hoodData, String width, String height)
    {
        return WidgetUtil.createFlashContainer(
            "hood","/media/static/HoodViz.swf", width, height,
            "skinURL= " + HOOD_SKIN_URL + "&neighborhood=" + hoodData);
    }

    public static HTML createPopularPlaces (String hotspotData)
    {
        return WidgetUtil.createFlashContainer(
            "hotspots","/media/static/HoodViz.swf", "100%", "550",
            "skinURL= " + HOOD_SKIN_URL + "&neighborhood=" + hotspotData);
    }
}
