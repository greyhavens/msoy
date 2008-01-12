//
// $Id$

package client.shell;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.threerings.gwt.ui.WidgetUtil;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import client.util.MsoyUI;
import client.util.Predicate;

/**
 * The frame wraps the top-level page HTML and handles displaying the navigation, the page content,
 * the client and sliding things around.
 */
public class Frame
{
    /** The maximum width of our content UI, the remainder is used by the world client. */
    public static final int CONTENT_WIDTH = 700;

    /** The width of the separator bar displayed between the client and the content. */
    public static final int SEPARATOR_WIDTH = 8;

    /** The offset of the content close button, from the left edge of the separator bar. */
    public static final int CLOSE_BUTTON_OFFSET = -16;

    /** Indicates whether we are currently displaying a Java applet over the parts of the page
     * where popups might show up. */
    public static boolean displayingJava = false;

    /**
     * Called by the Application to initialize us once in the lifetime of the app.
     */
    public static void init (NaviPanel navi, StatusPanel status)
    {
        // add the logo, with link to My Whirled/Whirledwide
        Image logo = new Image("/images/header/header_logo.png");
        logo.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                boolean loggedIn = CShell.creds != null;
                Application.go(Page.WHIRLED, loggedIn ? "mywhirled" : "whirledwide");
            }
        });

        RootPanel logoPanel = RootPanel.get("logo");
        if (logoPanel != null) {
            logoPanel.add(logo);
        }
        RootPanel statusPanel = RootPanel.get("status");
        if (statusPanel != null) {
            statusPanel.add(status);
        }
        RootPanel naviPanel = RootPanel.get(NAVIGATION);
        if (naviPanel != null) {
            naviPanel.add(navi);
        }

        _minimizeContent = MsoyUI.createActionLabel("", "Minimize", new ClickListener() {
            public void onClick (Widget sender) {
                setContentMinimized(true, null);
            }
        });
        _maximizeContent = MsoyUI.createActionLabel("", "Maximize", new ClickListener() {
            public void onClick (Widget sender) {
                setContentMinimized(false, null);
            }
        });

        // set up the window to not scroll, Page will scroll just our page content
        Window.enableScrolling(false);

        // set up the callbackd that our flash clients can call
        configureCallbacks();
    }

    /**
     * Returns true if we need to do our popup hackery, false if not.
     */
    public static boolean needPopupHack ()
    {
        // if we're displaying a Java applet, we always need the popup hack, but for Flash we only
        // need it on Linux (we always assume there's some goddamned Flash on the page)
        return displayingJava || isLinux();
    }

    /**
     * Sets the title of the browser window and the page (displayed below the Whirled logo).
     */
    public static void setTitle (String title)
    {
        setTitle(title, null);
    }

    /**
     * Sets the title and subtitle of the browser window and the page. The subtitle is displayed to
     * the right of the title in the page and tacked onto the title for the browser window.
     */
    public static void setTitle (String title, String subtitle)
    {
        if (_content != null) {
            _content.setPageTitle(title, subtitle);
        }
        title = (subtitle == null) ? title : (title + "-" + subtitle);
        Window.setTitle(CShell.cmsgs.windowTitle(title));
    }

    /**
     * Minimizes or maximizes the page content. NOOP if the content min/max interface is not being
     * displayed.
     */
    public static void setContentMinimized (boolean minimized, Command onComplete)
    {
        if (minimized && _minimizeContent.isAttached()) {
            RootPanel.get(SEPARATOR).remove(_minimizeContent);
            RootPanel.get(SEPARATOR).add(_maximizeContent);
            new SlideContentOff().start(onComplete);

        } else if (!minimized && _maximizeContent.isAttached()) {
            RootPanel.get(SEPARATOR).remove(_maximizeContent);
            RootPanel.get(SEPARATOR).add(_minimizeContent);
            new SlideContentOn().start(onComplete);

        } else if (onComplete != null) {
            // no action needed, just run the onComplete
            onComplete.execute();
        }
    }

    /**
     * Switches the frame into client display mode (clearing out any content) and notes the history
     * token for the current page so that it can be restored in the event that we open a normal
     * page and then later close it.
     */
    public static void setShowingClient (String closeToken)
    {
        // note the current history token so that we can restore it if needed
        _closeToken = closeToken;

        // clear out our content and the expand/close controls
        RootPanel.get(CONTENT).clear();
        RootPanel.get(CONTENT).setWidth("0px");

        // clear out the divider
        RootPanel.get(SEPARATOR).clear();
        RootPanel.get(SEPARATOR).setWidth("0px");

        // have the client take up all the space
        RootPanel.get(CLIENT).setWidth("100%");
    }

    /**
     * Clears any open client and restores the content display.
     */
    public static void closeClient (boolean deferred)
    {
        if (deferred) {
            DeferredCommand.add(new Command() {
                public void execute () {
                    closeClient(false);
                }
            });
            return;
        }

        WorldClient.closeClient(true);
        _closeToken = null;
        RootPanel.get(SEPARATOR).clear();
    }

    /**
     * Clears the open content and restores the client to its full glory.
     */
    public static void closeContent ()
    {
        if (_closeToken == null) {
            return;
        }

        if (_maximizeContent.isAttached()) {
            RootPanel.get(SEPARATOR).remove(_maximizeContent);
            History.newItem(_closeToken);

        } else {
            new SlideContentOff().start(new Command() {
                public void execute () {
                    RootPanel.get(SEPARATOR).remove(_minimizeContent);
                    History.newItem(_closeToken);
                }
            });
        }

        _content = null;
        _contlist = null;
        _scroller = null;
    }

    /**
     * Requests that the specified widget be scrolled into view.
     */
    public static void ensureVisible (Widget widget)
    {
        if (_scroller != null) {
            _scroller.ensureVisible(widget);
        }
    }

    /**
     * Displays the supplied dialog in the frame.
     */
    public static void showDialog (String title, Widget dialog)
    {
        Dialog pd = new Dialog(title, dialog);
        if (_contlist != null) {
            _contlist.insert(pd, 0); // TODO: animate this sliding down
        } else {
            RootPanel.get(HEADER).add(pd); // TODO: animate this sliding down
        }
    }

    /**
     * Clears the specified dialog from the frame. Returns true if the dialog was located and
     * cleared, false if not.
     */
    public static boolean clearDialog (final Widget dialog)
    {
        return clearDialog(new Predicate() {
            public boolean isMatch (Object o) {
                return (o == dialog);
            }
        }) > 0;
    }

    /**
     * Clears all dialogs that match the specified predicate. Returns the number of dialogs
     * cleared.
     */
    public static int clearDialog (Predicate pred)
    {
        int removed = clearDialog(RootPanel.get(HEADER), pred);
        if (_contlist != null) {
            removed += clearDialog(_contlist, pred);
        }
        return removed;
    }

    /**
     * Configures the widget to be displayed in the content portion of the frame. Will animate the
     * content sliding on if appropriate.
     */
    protected static void setContent (Page.Content content, boolean contentIsJava)
    {
        RootPanel.get(CONTENT).clear();

        // clear out any lingering dialogs
        clearDialog(Predicate.TRUE);

        // note that this is our current content
        _contlist = new VerticalPanel();
        _contlist.setWidth("100%");
        _contlist.add(_content = content);
        _scroller = new ScrollPanel(_contlist);
        _scroller.setHeight((Window.getClientHeight() - 50) + "px");
        displayingJava = contentIsJava;

        // if we're displaying the client or we have a minimized page, unminimize things first
        if (_maximizeContent.isAttached() ||
            (_closeToken != null && !_minimizeContent.isAttached())) {
            RootPanel.get(SEPARATOR).clear();
            RootPanel.get(SEPARATOR).add(_minimizeContent);
            new SlideContentOn().start(null);

        } else {
            RootPanel.get(CONTENT).add(_scroller);
            RootPanel.get(CONTENT).setWidth(CONTENT_WIDTH + "px");
        }

        _content.setCloseVisible(RootPanel.get(CLIENT).getWidgetCount() > 0);
    }

    protected static void restoreClient ()
    {
        setContentMinimized(true, null);
    }

    protected static void setSeparator (int x)
    {
        clearSeparator();
        Label div = new Label();
        div.setStyleName("SeparatorFromFlash");
        DOM.setAttribute(div.getElement(), "id", "separatorFromFlash");
        DOM.setStyleAttribute(div.getElement(), "left", x + "px");
        RootPanel.get(NAVIGATION).add(div);
    }

    protected static void clearSeparator ()
    {
        Element div = DOM.getElementById("separatorFromFlash");
        if (div != null) {
            DOM.removeChild(DOM.getParent(div), div);
        }
    }

    protected static int clearDialog (ComplexPanel panel, Predicate pred)
    {
        int removed = 0;
        for (int ii = 0; ii < panel.getWidgetCount(); ii++) {
            Widget widget = panel.getWidget(ii);
            if (widget instanceof Dialog && pred.isMatch(((Dialog)widget).getContent())) {
                panel.remove(ii);
                removed++;
            }
        }
        return removed;
    }

    /**
     * Configures top-level functions that can be called by Flash.
     */
    protected static native void configureCallbacks () /*-{
       $wnd.restoreClient = function () {
            @client.shell.Frame::restoreClient()();
       };
       $wnd.clearClient = function () {
            @client.shell.Frame::closeClient(Z)(true);
       };
       $wnd.setSeparator = function (x) {
            @client.shell.Frame::setSeparator(I)(x);
       };
       $wnd.clearSeparator = function () {
            @client.shell.Frame::clearSeparator()();
       };
    }-*/;

    protected static native boolean isLinux () /*-{
        return (navigator.userAgent.toLowerCase().indexOf("linux") != -1);
    }-*/;

    protected static abstract class Slider extends Timer
    {
        public void start (Command onComplete) {
            _onComplete = onComplete;
//             scheduleRepeating(25);
            run();
        }

        protected void done () {
            cancel();
            if (_onComplete != null) {
                _onComplete.execute();
            }
        }

        protected Command _onComplete;
        protected static final int FRAMES = 6;
    }

    protected static class SlideContentOff extends Slider
    {
        public SlideContentOff () {
            RootPanel.get(CONTENT).clear();
            WorldClient.setMinimized(false);
            _content.setCloseVisible(false);
        }

        public void run () {
//             if (_startWidth >= _endWidth) {
                RootPanel.get(CONTENT).setWidth("0px");
                RootPanel.get(CLIENT).setWidth(_endWidth + "px");
                done();

//             } else {
//                 RootPanel.get(CONTENT).setWidth((_availWidth - _startWidth) + "px");
//                 RootPanel.get(CLIENT).setWidth(_startWidth + "px");
//                 _startWidth += _deltaWidth;
//             }
        }

        protected int _availWidth = Window.getClientWidth() - SEPARATOR_WIDTH;
        protected int _startWidth = Math.max(_availWidth - CONTENT_WIDTH, 0);
        protected int _endWidth = _availWidth;
        protected int _deltaWidth = (_endWidth - _startWidth) / FRAMES;
    }

    protected static class SlideContentOn extends Slider
    {
        public void run () {
//             if (_startWidth <= _endWidth) {
                RootPanel.get(CONTENT).add(_scroller);
                RootPanel.get(CONTENT).setWidth(CONTENT_WIDTH + "px");
                RootPanel.get(CLIENT).setWidth(_endWidth + "px");
                WorldClient.setMinimized(true);
                done();

//             } else {
//                 RootPanel.get(CONTENT).setWidth((_availWidth - _startWidth) + "px");
//                 RootPanel.get(CLIENT).setWidth(_startWidth + "px");
//                 _startWidth += _deltaWidth;
//             }
        }

        protected int _availWidth = Window.getClientWidth() - SEPARATOR_WIDTH;
        protected int _endWidth = Math.max(_availWidth - CONTENT_WIDTH, 0);
        protected int _startWidth = _availWidth;
        protected int _deltaWidth = (_endWidth - _startWidth) / FRAMES;
    }

    protected static class Dialog extends FlexTable
    {
        public Dialog (String title, Widget content)
        {
            setCellPadding(0);
            setCellSpacing(0);
            setStyleName("pageHeader");

            setText(0, 0, title);
            getFlexCellFormatter().setStyleName(0, 0, "TitleCell");

            setWidget(0, 1, MsoyUI.createActionLabel("", "CloseBox", new ClickListener() {
                public void onClick (Widget sender) {
                    Frame.clearDialog(getContent());
                }
            }));
            getFlexCellFormatter().setStyleName(0, 1, "CloseCell");

            setWidget(1, 0, content);
            getFlexCellFormatter().setColSpan(1, 0, 2);

            setWidget(2, 0, WidgetUtil.makeShim(5, 5));
            getFlexCellFormatter().setColSpan(2, 0, 2);
        }

        public Widget getContent ()
        {
            return getWidget(1, 0);
        }
    }

    protected static String _closeToken;

    protected static Page.Content _content;
    protected static VerticalPanel _contlist;
    protected static ScrollPanel _scroller;
    protected static Label _minimizeContent, _maximizeContent;

    // constants for our top-level elements
    protected static final String HEADER = "header";
    protected static final String NAVIGATION = "navigation";
    protected static final String CONTENT = "content";
    protected static final String SEPARATOR = "seppy";
    protected static final String CLIENT = "client";
}
