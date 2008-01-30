//
// $Id$

package client.util;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.web.client.DeploymentConfig;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;

/**
 * Utility methods for generating flash clients.
 */
public class FlashClients
{
    public static final String HOOD_SKIN_URL = "/media/static/hood_pastoral.swf";

    /**
     * Creates a world client, and embeds it in a container object, with which it can communicate
     * via the Flash/Javascript interface.
     */
    public static void embedWorldClient (Panel container, String flashVars)
    {
        WidgetUtil.embedFlashObject(
            container, WidgetUtil.createFlashObjectDefinition(
                "asclient", "/clients/" + DeploymentConfig.version + "/world-client.swf",
                "100%", String.valueOf(CLIENT_HEIGHT), flashVars));
    }

    /**
     * Creates a game client, and embeds it in a container object, with which it can communicate
     * via the Flash/Javascript interface.
     */
    public static void embedGameClient (Panel container, String flashVars)
    {
        WidgetUtil.embedFlashObject(
            container, WidgetUtil.createFlashObjectDefinition(
                "asclient", "/clients/" + DeploymentConfig.version + "/game-client.swf",
                "100%", String.valueOf(CLIENT_HEIGHT), flashVars));
    }

    /**
     * Creates a featured places world client, and embeds it in the container object.
     */
    public static void embedFeaturedPlaceView (Panel container, String flashVars)
    {
        WidgetUtil.embedFlashObject(
            container, WidgetUtil.createFlashObjectDefinition(
                "featuredplace", "/clients/" + DeploymentConfig.version + "/world-client.swf",
                FEATURED_PLACE_WIDTH, FEATURED_PLACE_HEIGHT, flashVars));
    }

    /**
     * Creates a decor viewer, and embeds it in the supplied HTML object which *must* be already
     * added to the DOM.
     */
    public static void embedDecorViewer (HTML html)
    {
        html.setHTML(WidgetUtil.createFlashObjectDefinition(
                         "decorViewer", "/clients/" + DeploymentConfig.version + "/decorviewer.swf",
                         600, 400, ""));
    }

    /**
     * Creates a neighborhood view definition, as an object definition string. The resulting
     * string can be turned into an embedded Flash object using a call to
     * WidgetUtil.embedFlashObject or equivalent.
     */
    public static String createPopularPlacesDefinition (String hotspotData)
    {
        return WidgetUtil.createFlashObjectDefinition(
            "hotspots", "/clients/" + DeploymentConfig.version + "/neighborhood.swf",
            "100%", String.valueOf(CLIENT_HEIGHT - BLACKBAR_HEIGHT),
            "skinURL= " + HOOD_SKIN_URL + "&neighborhood=" + hotspotData);
    }

    /**
     * Creates an avatar viewer without equipping it for communication with Javascript.
     */
    public static HTML createAvatarViewer (String avatarPath, float scale, boolean allowScaleChange)
    {
        String flashVars = "avatar=" + URL.encodeComponent(avatarPath) + "&scale=" + scale;
        if (allowScaleChange) {
            flashVars += "&scaling=true";
        }
        return WidgetUtil.createFlashContainer(
            "avatarViewer", "/clients/" + DeploymentConfig.version + "/avatarviewer.swf",
            360, 450, flashVars);
    }

    /**
     * Creates an video viewer without equipping it for communication with Javascript.
     */
    public static HTML createVideoViewer (String videoPath)
    {
        return WidgetUtil.createFlashContainer(
            "videoViewer", "/clients/" + DeploymentConfig.version + "/videoviewer.swf",
            320, 240, "video=" + URL.encodeComponent(videoPath));
    }

    /**
     * Checks if the flash client can be found on this page.
     */
    public static boolean clientExists ()
    {
        return clientExistsNative();
    }

    /**
     * Checks with the actionscript client to find out if our current scene is in fact a room.
     */
    public static boolean inRoom ()
    {
        return inRoomNative();
    }

    /**
     * Returns true if the item in question is in use, false if not.
     */
    public static boolean isItemInUse (Item item)
    {
        return isItemInUseNative(item.getType(), item.itemId);
    }

    /**
     * Tells the actionscript client that we'd like to use this item in the current room.  This can
     * be used to add furni, or set the background audio or decor.
     */
    public static void useItem (byte itemType, int itemId)
    {
        useItemNative(itemType, itemId);
    }

    /**
     * Tells the actionscript client to remove the given item from use.
     */
    public static void clearItem (byte itemType, int itemId)
    {
        clearItemNative(itemType, itemId);
    }

    /**
     * Tells the actionscript client that we'd like to use this avatar.  If 0 is passed in for the
     * avatarId, the current avatar is simply cleared away, leaving you tofulicious.
     */
    public static void useAvatar (int avatarId, float scale)
    {
        useAvatarNative(avatarId, scale);
    }

    /**
     * Informs the client of a tutorial event.
     */
    public static void tutorialEvent (String eventName)
    {
        tutorialEventNative(eventName);
    }

    /**
     * Helpy helper function.
     */
    public static native int getLength (JavaScriptObject array) /*-{
        return array.length;
    }-*/;

    /**
     * Helpy helper function.  Makes it possible to retrieve objects from arrays that are members
     * of arrays.
     */
    public static native JavaScriptObject getJavaScriptElement (JavaScriptObject array,
        int index) /*-{
        return array[index];
    }-*/;

    /**
     * Helpy helper function.
     */
    public static native String getStringElement (JavaScriptObject array, int index) /*-{
        return array[index];
    }-*/;

    /**
     * Helpy helper function.
     */
    public static native int getIntElement (JavaScriptObject array, int index) /*-{
        return array[index];
    }-*/;

    /**
     * Helpy helper function.
     */
    public static native byte getByteElement (JavaScriptObject array, int index) /*-{
        return array[index];
    }-*/;

    /**
     * Helpy helper function.
     */
    public static native boolean getBooleanElement (JavaScriptObject array, int index) /*-{
        return array[index];
    }-*/;

    /**
     * Helpy helper function.
     */
    public static native boolean getBoolean (JavaScriptObject value) /*-{
        return value;
    }-*/;

    /**
     * Does the actual <code>clientExists()</code> call.
     */
    protected static native boolean clientExistsNative () /*-{
        return $doc.getElementById("asclient") != null;
    }-*/;

    /**
     * Does the actual <code>inRoom()</code> call.
     */
    protected static native boolean inRoomNative () /*-{
        var client = $doc.getElementById("asclient");
        if (client) {
            return client.inRoom();
        }
        return false;
    }-*/;

    /**
     * Does the actual <code>isItemInUse()</code> call.
     */
    protected static native boolean isItemInUseNative (byte itemType, int itemId) /*-{
        var client = $doc.getElementById("asclient");
        if (client) {
            return client.isItemInUse(itemType, itemId);
        }
        return 0;
    }-*/;

    /**
     * Does the actual <code>useItem()</code> call.
     */
    protected static native void useItemNative (byte itemType, int itemId) /*-{
        var client = $doc.getElementById("asclient");
        if (client) {
            client.useItem(itemType, itemId);
        }
    }-*/;

    /**
     * Does the actual <code>clearItem()</code> call.
     */
    protected static native void clearItemNative (byte itemType, int itemId) /*-{
        var client = $doc.getElementById("asclient");
        if (client) {
            client.clearItem(itemType, itemId);
        }
    }-*/;

    /**
     * Does the actual <code>useAvatar()</code> call.
     */
    protected static native void useAvatarNative (int avatarId, float scale) /*-{
        var client = $doc.getElementById("asclient");
        if (client) {
            client.useAvatar(avatarId, scale);
        }
    }-*/;

    /**
     * Does the actual <code>tutorialEvent()</code> call.
     */
    protected static native void tutorialEventNative (String eventName) /*-{
        var client = $doc.getElementById("asclient");
        if (client) {
            client.tutorialEvent(eventName);
        }
    }-*/;

    // TODO: put this in Application?
    protected static final int HEADER_HEIGHT = 50;
    protected static final int BLACKBAR_HEIGHT = 20;
    protected static final int CLIENT_HEIGHT = 544;

    protected static final String FEATURED_PLACE_WIDTH = "424px";
    protected static final String FEATURED_PLACE_HEIGHT = "200px";
}
