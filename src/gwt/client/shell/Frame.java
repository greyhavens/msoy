//
// $Id$

package client.shell;

import com.google.gwt.user.client.ui.Widget;

/**
 * Provides a means by which the pages can interact with the frame.
 */
public interface Frame
{
    /** The height of our frame navigation header. */
    public static final int NAVI_HEIGHT = 50 /* header */;

    /** The height of our frame navigation header and page title bar. */
    public static final int HEADER_HEIGHT = NAVI_HEIGHT + 37 /* title bar */;

    /** The height of our Flash or Java client in pixels. */
    public static final int CLIENT_HEIGHT = 544;

    /** The maximum width of our content UI, the remainder is used by the world client. */
    public static final int CONTENT_WIDTH = 700;

    /** The offset of the content close button, from the left edge of the separator bar. */
    public static final int CLOSE_BUTTON_OFFSET = -16;

    /** Enumerates our different header tabs. */
    public static enum Tabs { ME, FRIENDS, GAMES, WHIRLEDS, SHOP, HELP };

    /** Codes for use with our inner frame to top frame RPC mechanism. */
    public static enum Calls {
        SET_TITLE, NAVIGATE_TO, NAVIGATE_REPLACE, DISPLAY_WORLD_CLIENT, CLOSE_CLIENT, CLOSE_CONTENT,
        GET_WEB_CREDS, GET_PAGE_TOKEN, GET_MD5, CHECK_FLASH_VERSION
    };

    /**
     * Sets the title of the browser window and the page.
     */
    void setTitle (String title);

    /**
     * Navigates to the page represented by the specified token.
     */
    void navigateTo (String token);

    /**
     * Replaces the current page with the page represented by the specified token, removing the
     * current page from the browser history in the process.
     */
    void navigateReplace (String token);

    /**
     * Displays the Flash world client with the specified args. If the client is already showing,
     * the args will be passed to the running client.
     *
     * @param pageToken a custom close token to override the default, or null for the default.
     */
    void displayWorldClient (String args, String closeToken);

    /**
     * Clears any open client and restores the content display.
     */
    void closeClient ();

    /**
     * Clears the open content and restores the client to its full glory.
     */
    void closeContent ();

    /**
     * Shows or hides the navigation header as desired.
     */
    void setHeaderVisible (boolean visible);

    /**
     * Requests that the specified widget be scrolled into view.
     */
    void ensureVisible (Widget widget);

    /**
     * Displays the supplied dialog in the frame.
     */
    void showDialog (String title, Widget dialog);

    /**
     * Displays the supplied dialog in the frame or floating over the page.
     */
    void showPopupDialog (String title, Widget dialog);

    /**
     * Hides the current dialog contents.
     */
    void clearDialog ();

    /**
     * Displays the supplied page content.
     */
    void showContent (Pages page, Widget pageContent);

//     /**
//      * Logs on with the supplied username and (unencrypted) password.
//      */
//     void logon (String username, String password);

    /**
     * MD5 encodes the supplied text.
     */
    String md5hex (String text);

    /**
     * Checks that the installed version of Flash is kosher. Returns null if so, a HTML snipped to
     * embed otherwise.
     */
    String checkFlashVersion (int width, int height);
}
