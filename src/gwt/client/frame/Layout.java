//
// $Id$

package client.frame;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.gwt.ClientMode;

/**
 * Handles the layout of our various frame elements (header, content, client).
 */
public abstract class Layout
{
    /**
     * Creates and returns a layout instance based on the given external arguments.
     */
    public static Layout create (
        FrameHeader header, ClientMode clientMode, boolean isInnerFrame, ClickHandler onGoHome)
    {
        Layout layout = null;
        if (clientMode.isFacebookGames()) {
            // TODO: rename FacebookLayout -> FacebookGamesPortalLayout or something
            layout = new FacebookGamesLayout();
        } else if (clientMode.isFacebookRooms()) {
            layout = new FacebookRoomsLayout();
        } else if (isInnerFrame) {
            layout = new FramedLayout();
        } else {
            layout = new StandardLayout();
        }
        layout.init(header, onGoHome);
        return layout;
    }

    /**
     * Checks if we are currently showing content.
     */
    public abstract boolean hasContent ();

    /**
     * Sets the current content page. Note that this also clears the bottom content if the layout
     * supports it.
     */
    public abstract void setContent (TitleBar bar, Widget content);

    /**
     * If supported, sets the widget as the content along the bottom, below the main content area.
     */
    public abstract void setBottomContent (Widget content);

    /**
     * Closes the content page, optionally restoring the client.
     */
    public abstract void closeContent (boolean restoreClient);

    /**
     * Prepares and returns the panel that will contain the world client.
     */
    public abstract Panel prepareClientPanel ();

    /**
     * Closes the client.
     */
    public abstract boolean closeClient ();

    /**
     * Adds the home button that is shown when the client is hidden. The click handler should be
     * the one used to initialize the layout.
     */
    public abstract void addNoClientIcon ();

    /**
     * Updates the layout after the title bar changes height.
     */
    public abstract void updateTitleBarHeight ();

    /**
     * Detects if this layout always shows the title bar. That is, the title bar is displayed even
     * when the client is active.
     */
    public boolean alwaysShowsTitleBar ()
    {
        return false;
    }

    /**
     * Sets the current title bar. Should only be called if the title bar is being shown when there
     * is no web page content. This in turn can only happen if {@link #alwaysShowsTitleBar()}
     * returns true.
     */
    public void setTitleBar (TitleBar bar)
    {
    }

    /**
     * Detects if this layout should adjust the title bar to look good in a frame.
     */
    public boolean usesFramedTitleBar ()
    {
        return false;
    }

    protected void init (FrameHeader header, ClickHandler onGoHome)
    {
        _header = header;
        _onGoHome = onGoHome;
    }

    protected FrameHeader _header;
    protected ClickHandler _onGoHome;

    protected static final String PAGE = "page";
}
