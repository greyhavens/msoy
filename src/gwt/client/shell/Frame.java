//
// $Id$

package client.shell;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.WindowResizeListener;

import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.gwt.util.Predicate;

import client.shell.images.NaviImages;
import client.util.MsoyUI;

/**
 * The frame wraps the top-level page HTML and handles displaying the navigation, the page content,
 * the client and sliding things around.
 */
public class Frame
{
    /** The height of our page header (just the menus and status stuff). */
    public static final int HEADER_HEIGHT = 50;

    /** The height of our Flash or Java client in pixels. */
    public static final int CLIENT_HEIGHT = 544;

    /** The maximum width of our content UI, the remainder is used by the world client. */
    public static final int CONTENT_WIDTH = 700;

    /** The width of the separator bar displayed between the client and the content. */
    public static final int SEPARATOR_WIDTH = 8;

    /** The offset of the content close button, from the left edge of the separator bar. */
    public static final int CLOSE_BUTTON_OFFSET = -16;

    /**
     * Called by the Application to initialize us once in the lifetime of the app.
     */
    public static void init ()
    {
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

        // create our default headers
        _mheader = new MemberHeader();
        _gheader = new GuestHeader();

        // if we're tall enough, handle scrolling ourselves
        Window.enableScrolling(windowTooShort());
        Window.addWindowResizeListener(_resizer);

        // set up the callbackd that our flash clients can call
        configureCallbacks();
    }

    /**
     * Called when the user logs on.
     */
    public static void didLogon ()
    {
        // create a new member header as it contains member specific stuff
        _mheader = new MemberHeader();

        // make sure the correct header is showing
        if (_gheader.isAttached()) {
            // this will remove the guest header and replace it with the member header
            setHeaderVisible(true);
        }
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
        title = (subtitle == null) ? title : (title + " - " + subtitle);
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

        // make sure the header is showing as we always want the header above the client
        setHeaderVisible(true);
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

        WorldClient.clientWillClose();
        _closeToken = null;
        RootPanel.get(SEPARATOR).clear();
        RootPanel.get(Frame.CLIENT).clear();
        RootPanel.get(Frame.CLIENT).setWidth("0px");
        RootPanel.get(Frame.CONTENT).setWidth("100%");
        _content.setCloseVisible(false);

        // if we're on a "world" page, go to the landing page
        if (History.getToken().startsWith(Page.WORLD)) {
            Application.go(Page.ME, "");
        }
    }

    /**
     * Clears the open content and restores the client to its full glory.
     *
     * @return true if the content was closed, false if we were not displaying content.
     */
    public static boolean closeContent ()
    {
        if (_closeToken == null) {
            return false;
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
        return true;
    }

    /**
     * Shows or hides the navigation header as desired.
     */
    public static void setHeaderVisible (boolean visible)
    {
        RootPanel.get(HEADER).remove(_gheader);
        RootPanel.get(HEADER).remove(_mheader);
        if (visible) {
            RootPanel.get(HEADER).add((CShell.getMemberId() == 0) ? _gheader : _mheader);
        }
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
     * Configures the frame with our page's content table.
     */
    protected static void initContent (Page.Content content)
    {
        _content = content;
    }

    /**
     * Displays the supplied page content (which is generally the same as the table previously
     * configured via {@link #initContent}). Will animate the content sliding on if appropriate.
     */
    protected static void showContent (Widget page)
    {
        RootPanel.get(CONTENT).clear();

        // clear out any lingering dialogs
        clearDialog(Predicate.TRUE);

        // note that this is our current content
        _contlist = new FlowPanel();
        _contlist.setWidth("100%");
        _contlist.add(page);

        Widget content;
        // if we're not showing a Page.Content page or the browser is too short; don't try to do
        // our own custom scrolling, just let everything scroll
        if (windowTooShort() || page != _content) {
            content = _contlist;
            Window.enableScrolling(true);
        } else {
            content = (_scroller = new ScrollPanel(_contlist));
            _scroller.setHeight((Window.getClientHeight() - HEADER_HEIGHT) + "px");
            Window.enableScrolling(false);
        }

        // if we're displaying the client or we have a minimized page, unminimize things first
        if (_maximizeContent.isAttached() ||
            (_closeToken != null && !_minimizeContent.isAttached())) {
            RootPanel.get(SEPARATOR).clear();
            RootPanel.get(SEPARATOR).add(_minimizeContent);
            new SlideContentOn().start(null);

        } else {
            RootPanel.get(CONTENT).add(content);
            RootPanel.get(CONTENT).setWidth(CONTENT_WIDTH + "px");
        }

        _content.setCloseVisible(RootPanel.get(CLIENT).getWidgetCount() > 0);
    }

    protected static boolean windowTooShort ()
    {
        return Window.getClientHeight() < (HEADER_HEIGHT + CLIENT_HEIGHT);
    }

    protected static void restoreClient ()
    {
        setContentMinimized(true, null);
    }

    protected static int clearDialog (ComplexPanel panel, Predicate pred)
    {
        if (panel == null) {
            return 0; // cope with stale index.html files
        }
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
                if (_scroller == null) {
                    RootPanel.get(CONTENT).add(_contlist);
                } else {
                    RootPanel.get(CONTENT).add(_scroller);
                }
                RootPanel.get(CONTENT).setWidth(CONTENT_WIDTH + "px");
                RootPanel.get(CLIENT).setWidth(_endWidth + "px");
                WorldClient.setMinimized(true);
                _content.setCloseVisible(true);
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

    protected static class Dialog extends SmartTable
    {
        public Dialog (String title, Widget content) {
            super("pageHeader", 0, 0);
            setText(0, 0, title, 1, "TitleCell");
            setWidget(0, 1, MsoyUI.createActionLabel("", "CloseBox", new ClickListener() {
                public void onClick (Widget sender) {
                    Frame.clearDialog(getContent());
                }
            }), 1, "CloseCell");
            setWidget(1, 0, content, 2, null);
            setWidget(2, 0, WidgetUtil.makeShim(5, 5), 2, null);
        }

        public Widget getContent () {
            return getWidget(1, 0);
        }
    }

    protected static abstract class Header extends SmartTable
        implements ClickListener
    {
        public Header () {
            super("msoyHeader", 0, 0);
            setWidth("100%");
            String lpath = "/images/header/header_logo.png";
            setWidget(0, 0, MsoyUI.createActionImage(lpath, this), 1, "Logo");
        }
    }

    protected static class GuestHeader extends Header
    {
        public GuestHeader () {
            SmartTable signup = new SmartTable();
            signup.setWidget(0, 0, MsoyUI.createLabel("New to Whirled?", "New"));
            signup.setWidget(1, 0, Application.createLink("Sign up!", Page.ACCOUNT, "create"));
            setWidget(0, 1, signup, 1, "Signup");
            setWidget(0, 2, new LogonPanel(true), 1, "Logon");
        }

        public void onClick (Widget sender) {
            History.newItem(""); // go to the main page
        }
    }

    protected static class MemberHeader extends Header
    {
        public MemberHeader () {
            int col = 1;
            setWidget(0, col++, new NaviButton(CShell.cmsgs.menuMe(), _images.me(), _images.ome(),
                                               Page.ME, ""));
            String arg = Args.compose("f", CShell.getMemberId());
            setWidget(0, col++, new NaviButton(CShell.cmsgs.menuFriends(), _images.friends(),
                                               _images.ofriends(), Page.PEOPLE, arg));
            setWidget(0, col++, new NaviButton(CShell.cmsgs.menuWorlds(), _images.worlds(),
                                               _images.oworlds(), Page.WHIRLEDS, ""));
            setWidget(0, col++, new NaviButton(CShell.cmsgs.menuGames(), _images.games(),
                                               _images.ogames(), Page.GAMES, ""));
            setWidget(0, col++, new NaviButton(CShell.cmsgs.menuShop(), _images.shop(),
                                               _images.oshop(), Page.SHOP, ""));
            setWidget(0, col++, new NaviButton(CShell.cmsgs.menuHelp(), _images.help(),
                                               _images.ohelp(), Page.ME, "help"));

            setWidget(0, col++, WidgetUtil.makeShim(156, 10), 1, "Left");
            getFlexCellFormatter().setHorizontalAlignment(0, col, HasAlignment.ALIGN_RIGHT);
            setWidget(0, col++, CShell.app.getStatusPanel(), 1, "Right");
        }

        public void onClick (Widget sender) {
            // if the world is open, close the content, otherwise go home
            if (!Frame.closeContent()) {
                Application.go(Page.WORLD, "h");
            }
        }
    }

    protected static class NaviButton extends Label
    {
        public NaviButton (String text, AbstractImagePrototype upImage,
                           AbstractImagePrototype overImage, final String page, final String args) {
            setStyleName("Button");

            _upImage = upImage.createImage();
            _overImage = overImage.createImage();

            addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    Application.go(page, args);
                }
            });
            setText(text);
            setBackgroundImage(_upImage);
        }

        protected void setBackgroundImage (Image image) {
            int left = -image.getOriginLeft(), top = -image.getOriginTop();
            String bgstr = "url('" + image.getUrl() + "') " + left + "px " + top + "px";
            DOM.setStyleAttribute(getElement(), "background", bgstr);
        }

        protected Image _upImage, _overImage;
    }

    protected static WindowResizeListener _resizer = new WindowResizeListener() {
        public void onWindowResized (int width, int height) {
            if (_scroller != null) {
                _scroller.setHeight((Window.getClientHeight() - HEADER_HEIGHT) + "px");
            }
        }
    };

    protected static Header _mheader, _gheader;
    protected static String _closeToken;

    protected static Page.Content _content;
    protected static FlowPanel _contlist;
    protected static ScrollPanel _scroller;
    protected static Label _minimizeContent, _maximizeContent;

    /** Our navigation menu images. */
    protected static NaviImages _images = (NaviImages)GWT.create(NaviImages.class);

    // constants for our top-level elements
    protected static final String HEADER = "header";
    protected static final String CONTENT = "content";
    protected static final String SEPARATOR = "seppy";
    protected static final String CLIENT = "client";
}
