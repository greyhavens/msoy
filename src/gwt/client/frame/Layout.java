//
// $Id$

package client.frame;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

import client.shell.Frame;

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
     * running by inspecting the DOM.
     */
    public static Layout getLayout (FrameHeader header, ClickHandler onGoHome)
    {
        Layout layout = isFramed() ? new FramedLayout() : new StandardLayout();
        layout.init(header, onGoHome);
        return layout;
    }

    public abstract boolean haveContent ();

    public abstract void setContent (TitleBar bar, Widget content);

    public abstract boolean closeContent (boolean restoreClient);

    public abstract WorldClient.PanelProvider getClientProvider ();

    public abstract boolean closeClient ();

    public abstract void addNoClientIcon ();

    protected void init (FrameHeader header, ClickHandler onGoHome)
    {
        _header = header;
        _onGoHome = onGoHome;
    }

    public native static boolean isFramed () /*-{
        return $wnd.top != $wnd;
    }-*/;

    protected FrameHeader _header;
    protected ClickHandler _onGoHome;

    protected static final String PAGE = "page";
}
