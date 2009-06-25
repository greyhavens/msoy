//
// $Id$

package client.frame;

import client.shell.Frame;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.data.all.DeploymentConfig;

/**
 * Handles the layout of our various frame elements (header, content, client).
 */
public abstract class Layout
{
    /** The height of our frame navigation header. */
    public static final int NAVI_HEIGHT = 50 /* header */;

    /** The height of our frame navigation header and page title bar. */
    public static final int HEADER_HEIGHT = NAVI_HEIGHT + 37 /* title bar */;

    /** The maximum width of our content UI, the remainder is used by the world client. */
    public static final int CONTENT_WIDTH = 700;

    /**
     * Creates and returns a layout instance based on what we can figure out about where we're
     * running by inspecting the DOM, or by checking for specific embed values.
     */
    public static Layout getLayout (FrameHeader header, String embedCookie, ClickHandler onGoHome)
    {
        Layout layout = null;
        Frame.Embedding embedding = Frame.Embedding.NONE;
        if (isFramed()) {
            if ("fb".equals(embedCookie)) {
                embedding = Frame.Embedding.FACEBOOK;
                layout = new FacebookLayout();
            }
            if (layout == null) {
                layout = new FramedLayout();
            }
        } else {
            layout = new StandardLayout();
        }
        layout.init(header, onGoHome);
        layout._embedding = embedding;
        return layout;
    }

    /**
     * Checks if we are currently showing content.
     */
    public abstract boolean hasContent ();

    /**
     * Sets the current content page.
     */
    public abstract void setContent (TitleBar bar, Widget content);

    /**
     * Closes the content page, optionally restoring the client.
     */
    public abstract void closeContent (boolean restoreClient);

    /**
     * Gets the provider that will service the opening of the world client.
     */
    public abstract WorldClient.PanelProvider getClientProvider ();

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
     * Determines the kind of embedding this layout is in.
     */
    public Frame.Embedding getEmbedding ()
    {
        return _embedding;
    }

    protected void init (FrameHeader header, ClickHandler onGoHome)
    {
        _header = header;
        _onGoHome = onGoHome;
    }

    protected native static boolean isFramed () /*-{
        return $wnd.top != $wnd;
    }-*/;

    protected FrameHeader _header;
    protected ClickHandler _onGoHome;
    protected Frame.Embedding _embedding;

    protected static final String PAGE = "page";
}
