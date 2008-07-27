//
// $Id$

package client.util;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.data.all.DeploymentConfig;

import client.shell.Frame;

/**
 * Utility methods for generating flash clients.
 */
public class FlashClients
{
    /** Our required minimum flash client version. */
    public static final int[] MIN_FLASH_VERSION = {9, 0, 115, 0};

    /**
     * Configure the WidgetUtil to use the appropriate flash player version.
     */
    static {
        WidgetUtil.FLASH_VERSION =  "" + MIN_FLASH_VERSION[0] + "," + MIN_FLASH_VERSION[1] +
                                    "," + MIN_FLASH_VERSION[2] + "," + MIN_FLASH_VERSION[3];
    }

    /**
     * Creates a world client, and embeds it in a container object, with which it can communicate
     * via the Flash/Javascript interface.
     */
    public static void embedWorldClient (Panel container, String flashVars)
    {
        if (shouldShowFlash(container, 0, 0)) {
            Widget embed = WidgetUtil.embedFlashObject(
                container, WidgetUtil.createFlashObjectDefinition(
                    "asclient", "/clients/" + DeploymentConfig.version + "/world-client.swf",
                    "100%", getClientHeight(), flashVars));
            embed.setHeight("100%");
        }
    }

    /**
     * Creates a game client, and embeds it in a container object, with which it can communicate
     * via the Flash/Javascript interface.
     */
    public static void embedGameClient (Panel container, String flashVars)
    {
        if (shouldShowFlash(container, 0, 0)) {
            WidgetUtil.embedFlashObject(
                container, WidgetUtil.createFlashObjectDefinition(
                    "asclient", "/clients/" + DeploymentConfig.version + "/game-client.swf",
                    "100%", getClientHeight(), flashVars));
        }
    }

    /**
     * Creates a featured places world client, and embeds it in the container object.
     */
    public static void embedFeaturedPlaceView (Panel container, String flashVars)
    {
        if (shouldShowFlash(container, FEATURED_PLACE_WIDTH, FEATURED_PLACE_HEIGHT)) {
            WidgetUtil.embedFlashObject(
                container, WidgetUtil.createFlashObjectDefinition(
                    "featuredplace", "/clients/" + DeploymentConfig.version + "/world-client.swf",
                    FEATURED_PLACE_WIDTH, FEATURED_PLACE_HEIGHT, flashVars));
        }
    }

    /**
     * Creates a decor viewer, and embeds it in the supplied HTML object which *must* be already
     * added to the DOM.
     */
    public static void embedDecorViewer (HTML html)
    {
        String definition = getUpgradeString(600, 400);
        html.setHTML(definition != null ? definition : WidgetUtil.createFlashObjectDefinition(
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
        String definition = getUpgradeString(0,0);
        return definition != null ? definition : WidgetUtil.createFlashObjectDefinition(
            "hotspots", "/clients/" + DeploymentConfig.version + "/neighborhood.swf",
            "100%", String.valueOf(Frame.CLIENT_HEIGHT - BLACKBAR_HEIGHT),
            "skinURL= " + HOOD_SKIN_URL + "&neighborhood=" + hotspotData);
    }

    /**
     * Creates a solo game definition, as an object definition string.
     */
    public static String createSoloGameDefinition (String media)
    {
        String definition = getUpgradeString(800, 600);
        return definition != null ? definition :
            WidgetUtil.createFlashObjectDefinition("game", media, 800, 600, null);
    }

    /**
     * Returns some code to display if flash isn't configured properly, or null if the default
     * flash code should be used.
     */
    public static String getUpgradeString (int width, int height)
    {
        // If they have the required flash, we're happy
        // If they're using IE, we'll let it handle upgrading/installing the flash activex control
        // since that works in most cases and we can't easily detect the cases where it doesn't
        if (hasFlashVersionNative(FULL_VERSION) || isIeNative()) {
            return null;
        }
        if (isVistaNative()) {
            // Only flash 9 is fully compatible with the express installer
            if (hasFlashVersionNative(VISTA_VERSION)) {
                return getExpressInstallerString(width, height);
            }

            // otherwise they'll need to do a manual upgrade
            return getFlashDownloadString(width, height);
        }
        // some mac flash plugins are booched and can't express install
        boolean mac = isMacNative();
        if (mac && !hasFlashVersionNative(MAC_PASSTHROUGH_MAX) &&
                hasFlashVersionNative(MAC_PASSTHROUGH_MIN)) {
            return null;
        }
        // only some system can run the upgrade script
        if (hasFlashVersionNative(UPGRADE_VERSION) && (mac || isWindowsNative())) {
            return getExpressInstallerString(width, height);
        }
        // if they have any version of flash we need to show the manual link
        if (hasFlashVersionNative(ANY_VERSION)) {
            return getFlashDownloadString(width, height);
        }
        // otherwise we'll rely on the browser to do the right thing when they don't have the plugin
        return null;
    }

    /**
     * Toggles the height 100% state of the client.
     */
    public static void toggleClientHeight ()
    {
        if (_clientFullHeight = !_clientFullHeight) {
            setClientHeightNative("100%");
        } else {
            setClientHeightNative(Frame.CLIENT_HEIGHT+"px");
        }
    }

    /**
     * Checks if the flash client can be found on this page.
     */
    public static boolean clientExists ()
    {
        return clientExistsNative();
    }

    /**
     * Get the current sceneId of the flash client, or 0.
     */
    public static int getSceneId ()
    {
        return getSceneIdNative();
    }

    /**
     * Checks with the actionscript client to find out if our current scene is in fact a room.
     */
    public static boolean inRoom ()
    {
        return inRoomNative();
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
     * Checks if we have a specilized flash object to show, and if so, adds it to the container
     * and returns false, otherwise returns true.
     */
    protected static boolean shouldShowFlash (Panel container, int width, int height)
    {
        String definition = getUpgradeString(width, height);
        if (definition != null) {
            WidgetUtil.embedFlashObject(container, definition);
            return false;
        }
        return true;
    }

    /**
     * Creates an embed string for the express installer swf.
     */
    protected static String getExpressInstallerString (int width, int height)
    {
        if (width < MIN_INSTALLER_WIDTH) {
            width = MIN_INSTALLER_WIDTH;
        }
        if (height < MIN_INSTALLER_HEIGHT) {
            height = MIN_INSTALLER_HEIGHT;
        }
        String flashArgs = "MMredirectURL=" + getLocationNative() + "&MMplayerType=PlugIn"
            + "&MMdoctitle=" + Window.getTitle().substring(0, 47) + " - Flash Player Installation";
        return WidgetUtil.createFlashObjectDefinition(
                "expressInstall", "/expressinstall/expressInstall.swf", width, height, flashArgs);
    }

    /**
     * Creates a string with a link to the flash player download site.
     */
    protected static String getFlashDownloadString (int width, int height)
    {
        String dims = (width > 0 && height > 0) ?
            " style=\"width:" + width + "px; height:" + height + "px\"" : "";
        return "<div class=\"flashLink\"" + dims + ">Whirled requires the latest " +
               "<a href=\"http://www.adobe.com/go/getflashplayer\" target=\"_blank\">" +
               "Flash Player</a> to operate. " +
               "<a href=\"http://www.adobe.com/go/getflashplayer\" target=\"_blank\">" +
               "<img src=\"/images/get_flash.jpg\"></div>";
    }

    /**
     * Returns the height to use for the world/game client.
     */
    protected static String getClientHeight ()
    {
        return _clientFullHeight ? "100%" : (""+Frame.CLIENT_HEIGHT);
    }

    /**
     * Does the actual <code>clientExists()</code> call.
     */
    protected static native boolean clientExistsNative () /*-{
        return $doc.getElementById("asclient") != null;
    }-*/;

    /**
     * TEMP: Changes the height of the client already embedded in the page.
     */
    protected static native void setClientHeightNative (String height) /*-{
        var client = $doc.getElementById("asclient");
        if (client != null) {
            client.style.height = height;
        }
    }-*/;

    /**
     * Does the actual <code>getSceneId()</code> call.
     */
    protected static native int getSceneIdNative () /*-{
        var client = $doc.getElementById("asclient");
        if (client) {
            // exception from JavaScript break GWT; don't let that happen
            try { return client.getSceneId(); } catch (e) {}
        }
        return 0;
    }-*/;

    /**
     * Does the actual <code>inRoom()</code> call.
     */
    protected static native boolean inRoomNative () /*-{
        var client = $doc.getElementById("asclient");
        if (client) {
            // exception from JavaScript break GWT; don't let that happen
            try { return client.inRoom(); } catch (e) {}
        }
        return false;
    }-*/;

    /**
     * Does the actual <code>useItem()</code> call.
     */
    protected static native void useItemNative (byte itemType, int itemId) /*-{
        var client = $doc.getElementById("asclient");
        if (client) {
            // exception from JavaScript break GWT; don't let that happen
            try { client.useItem(itemType, itemId); } catch (e) {}
        }
    }-*/;

    /**
     * Does the actual <code>clearItem()</code> call.
     */
    protected static native void clearItemNative (byte itemType, int itemId) /*-{
        var client = $doc.getElementById("asclient");
        if (client) {
            // exception from JavaScript break GWT; don't let that happen
            try { client.clearItem(itemType, itemId); } catch (e) {}
        }
    }-*/;

    /**
     * Does the actual <code>useAvatar()</code> call.
     */
    protected static native void useAvatarNative (int avatarId, float scale) /*-{
        var client = $doc.getElementById("asclient");
        if (client) {
            // exception from JavaScript break GWT; don't let that happen
            try { client.useAvatar(avatarId, scale); } catch (e) {}
        }
    }-*/;

    /**
     * Does the actual <code>tutorialEvent()</code> call.
     */
    protected static native void tutorialEventNative (String eventName) /*-{
        var client = $doc.getElementById("asclient");
        if (client) {
            // exception from JavaScript break GWT; don't let that happen
            try { client.tutorialEvent(eventName); } catch (e) {}
        }
    }-*/;

    /**
     * Checks for a minimum flash version.
     */
    protected static native boolean hasFlashVersionNative (String version) /*-{
        return $wnd.swfobject.hasFlashPlayerVersion(version);
    }-*/;

    /**
     * Gets the current location.
     */
    protected static native String getLocationNative () /*-{
        return $wnd.location;
    }-*/;

    /**
     * Returns true if we're in windows vista.
     */
    protected static native boolean isVistaNative () /*-{
        var u = $wnd.navigator.userAgent.toLowerCase();
        return /windows nt 6.0/.test(u);
    }-*/;

    /**
     * Returns true if we're in windows.
     */
    protected static native boolean isWindowsNative () /*-{
        return $wnd.swfobject.ua.win;
    }-*/;

    /**
     * Returns true if we're in internet explorer.
     */
    protected static native boolean isIeNative () /*-{
        return $wnd.swfobject.ua.ie;
    }-*/;

    /**
     * Returns true if we're on a mac.
     */
    protected static native boolean isMacNative () /*-{
        return $wnd.swfobject.ua.mac;
    }-*/;

    /** TEMP: Whether or not the client is in full-height mode. */
    protected static boolean _clientFullHeight = false;

    // TODO: put this in Application?
    protected static final int BLACKBAR_HEIGHT = 20;

    protected static final String HOOD_SKIN_URL = "/media/static/hood_pastoral.swf";
    protected static final int FEATURED_PLACE_WIDTH = 380;
    protected static final int FEATURED_PLACE_HEIGHT = 167;

    protected static final int MIN_INSTALLER_WIDTH = 310;
    protected static final int MIN_INSTALLER_HEIGHT = 137;

    /** The minimum flash for full functionality. */
    protected static final String FULL_VERSION =
        "" + MIN_FLASH_VERSION[0] + "." + MIN_FLASH_VERSION[1] + "." + MIN_FLASH_VERSION[2];

    /** The minimum flash for the express upgrade in vista. */
    protected static final String VISTA_VERSION = "9.0.0";

    /** A range of mac versions we'll let through since they have problems with express install. */
    protected static final String MAC_PASSTHROUGH_MIN = "9.0.0";
    protected static final String MAC_PASSTHROUGH_MAX = "9.0.49";

    /** The minimum flash for the express upgrade functionality. */
    protected static final String UPGRADE_VERSION = "6.0.65";

    /** See if they have any flash at all. */
    protected static final String ANY_VERSION = "0.0.1";
}
