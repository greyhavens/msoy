//
// $Id$

package client.shell;

import com.google.gwt.user.client.ui.Panel;
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

    /**
     * Sets the title of the browser window and the page.
     */
    void setTitle (String title);

    /**
     * Switches the frame into client display mode (clearing out any content) and notes the history
     * token for the current page so that it can be restored in the event that we open a normal
     * page and then later close it.
     */
    void setShowingClient (String closeToken);

    /**
     * Clears any open client and restores the content display.
     */
    void closeClient (boolean deferred);

    /**
     * Clears the open content and restores the client to its full glory.
     *
     * @return true if the content was closed, false if we were not displaying content.
     */
    boolean closeContent ();

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
     * Clears out the client section of the frame and creates a new scroll pane to contain a new
     * client (and other bits if desired).
     */
    Panel getClientContainer ();

    /**
     * Displays the supplied page content.
     */
    void showContent (String pageId, Widget pageContent);
}
