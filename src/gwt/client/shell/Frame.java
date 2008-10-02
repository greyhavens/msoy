//
// $Id$

package client.shell;

import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.data.Invitation;
import com.threerings.msoy.web.data.SessionData;

import client.util.events.FlashEvent;

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
    public static final int CLIENT_HEIGHT = 545;

    /** The maximum width of our content UI, the remainder is used by the world client. */
    public static final int CONTENT_WIDTH = 700;

    /** The offset of the content close button, from the left edge of the separator bar. */
    public static final int CLOSE_BUTTON_OFFSET = -16;

    /** Enumerates our different header tabs. */
    public static enum Tabs { ME, STUFF, GAMES, WHIRLEDS, SHOP, /*CREATE,*/ HELP };

    /** Codes for use with our inner frame to top frame RPC mechanism. */
    public static enum Calls {
        SET_TITLE, ADD_NAV_LINK, NAVIGATE_TO, NAVIGATE_REPLACE, CLOSE_CLIENT, CLOSE_CONTENT,
        DID_LOGON, GET_WEB_CREDS, GET_PAGE_TOKEN, GET_MD5, CHECK_FLASH_VERSION, GET_ACTIVE_INVITE,
        GET_VISITOR_INFO
    };

    /**
     * Sets the title of the browser window and the page.
     */
    void setTitle (String title);

    /**
     * Adds an additional link to the title bar sub-navigation. When the history changes, the
     * sub-navigation is reset to its default, so a page should set up any custom sub-navigation
     * every time the history changes.
     */
    void addNavLink (String label, Pages page, String args, int position);

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
     * Clears any open client and restores the content display.
     */
    void closeClient ();

    /**
     * Clears the open content and restores the client to its full glory.
     */
    void closeContent ();

    /**
     * Displays the supplied dialog.
     */
    void showDialog (String title, Widget dialog);

    /**
     * Hides the current dialog contents.
     */
    void clearDialog ();

    /**
     * Dispatches a Flash event to all registered listeners.
     */
    void dispatchEvent (FlashEvent event);

    /**
     * This should be called by any entity that logs us on.
     */
    void dispatchDidLogon (SessionData data);

    /**
     * MD5 encodes the supplied text.
     */
    String md5hex (String text);

    /**
     * Checks that the installed version of Flash is kosher. Returns null if so, a HTML snipped to
     * embed otherwise.
     */
    String checkFlashVersion (int width, int height);

    /**
     * Returns the invitation via which the current user arrived or null.
     */
    Invitation getActiveInvitation ();
}
