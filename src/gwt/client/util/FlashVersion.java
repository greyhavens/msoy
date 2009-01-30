//
// $Id$

package client.util;

import com.google.gwt.user.client.Window;

import com.threerings.gwt.ui.WidgetUtil;

/**
 * Utility methods for checking the browser's current Flash version.
 */
public class FlashVersion
{
    /** Our required minimum flash client version. */
    public static final int[] MIN_FLASH_VERSION = { 9, 0, 115, 0 };

    /**
     * Configure the WidgetUtil to use the appropriate flash player version.
     */
    static {
        WidgetUtil.FLASH_VERSION =  "" + MIN_FLASH_VERSION[0] + "," + MIN_FLASH_VERSION[1] +
                                    "," + MIN_FLASH_VERSION[2] + "," + MIN_FLASH_VERSION[3];
    }

    /**
     * Checks that the Flash player that is installed is sufficiently new.
     * 
     * @return null if everything is OK, or a string of HTML to display instead of the Flash client 
     * in the event that the player is not installed or out of date.
     */
    public static String checkFlashVersion (int width, int height)
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
                "expressInstall", "/expressinstall/playerProductInstall.swf",
                width, height, flashArgs);
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
     * Checks for a minimum flash version.
     */
    protected static native boolean hasFlashVersionNative (String version) /*-{
        return $wnd.swfobject.hasFlashPlayerVersion(version);
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

    /**
     * Gets the current location.
     */
    protected static native String getLocationNative () /*-{
        return $wnd.location;
    }-*/;

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

    protected static final int MIN_INSTALLER_WIDTH = 310;
    protected static final int MIN_INSTALLER_HEIGHT = 137;
}
