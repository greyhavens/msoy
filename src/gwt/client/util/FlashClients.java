//
// $Id$

package client.util;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.ui.HTML;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.web.data.FriendEntry;
import com.threerings.msoy.web.data.MemberName;

/**
 * Utility methods for generating flash clients.
 */
public class FlashClients
{
    public static final String HOOD_SKIN_URL = "/media/static/hood_pastoral.swf";

    public static HTML createWorldClient (String flashVars)
    {
        return WidgetUtil.createFlashContainer(
            "asclient", "/clients/world-client.swf", "90%", "550", flashVars);
    }

    public static HTML createHeaderClient (String token)
    {
        return WidgetUtil.createFlashContainer(
            "asclient", "/clients/header-client.swf", "5", "5", "token=" + token);
    }

    public static HTML createLobbyClient (int gameId, String token)
    {
        return WidgetUtil.createFlashContainer(
            "asclient", "/clients/world-client.swf", 800, 600,
            "gameLobby=" + gameId + "&token=" + token);
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

    /**
     * Calls into the Flash client and gets the list of our friends.
     */
    public static FriendEntry[] getFriends ()
    {
        JavaScriptObject result = getFriendsNative();
        int length = (result == null ? 0 : getLength(result)/3);
        FriendEntry[] friends = new FriendEntry[length];
        for (int ii = 0; ii < friends.length; ii++) {
            friends[ii] = new FriendEntry();
            friends[ii].name = new MemberName(getStringElement(result, 3*ii),
                                              getIntElement(result, 3*ii+1));
            friends[ii].online = getBooleanElement(result, 3*ii+2);
            // status is always full-fledged friend
            friends[ii].status = FriendEntry.FRIEND;
        }
        return friends;
    }

    /**
     * Does the actual JavaScript <code>getFriends</code> call.
     */
    protected static native JavaScriptObject getFriendsNative () /*-{
        var client = $doc.getElementById("asclient");
        return (client) ? client.getFriends() : null;
    }-*/;

    /**
     * Helpy helper function.
     */
    protected static native int getLength (JavaScriptObject array) /*-{
        return array.length;
    }-*/;

    /**
     * Helpy helper function.
     */
    protected static native String getStringElement (JavaScriptObject array, int index) /*-{
        return array[index];
    }-*/;

    /**
     * Helpy helper function.
     */
    protected static native int getIntElement (JavaScriptObject array, int index) /*-{
        return array[index];
    }-*/;

    /**
     * Helpy helper function.
     */
    protected static native boolean getBooleanElement (JavaScriptObject array, int index) /*-{
        return array[index];
    }-*/;
}
