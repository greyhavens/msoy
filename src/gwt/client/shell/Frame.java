//
// $Id$

package client.shell;

import java.util.ArrayList;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.WindowResizeListener;

import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MouseListenerAdapter;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.gwt.util.Predicate;

import client.shell.images.NaviImages;
import client.util.MsoyUI;

/**
 * The frame wraps the top-level page HTML and handles displaying the navigation, the page content,
 * and the various clients.
 */
public class Frame
{
    /** The height of our frame header and page title bar. */
    public static final int HEADER_HEIGHT = 50 /* header */ + 37 /* title bar */;

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
                setContentMinimized(true);
            }
        });
        _maximizeContent = MsoyUI.createActionLabel("", "Maximize", new ClickListener() {
            public void onClick (Widget sender) {
                setContentMinimized(false);
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
     * Sets the title of the browser window and the page.
     */
    public static void setTitle (String title)
    {
        if (_bar != null) {
            _bar.setTitle(title);
        }
        Window.setTitle(CShell.cmsgs.windowTitle(title));
    }

    /**
     * Minimizes or maximizes the page content. NOOP if the content min/max interface is not being
     * displayed.
     */
    public static void setContentMinimized (boolean minimized)
    {
        if (minimized && _minimizeContent.isAttached()) {
            RootPanel.get(SEPARATOR).remove(_minimizeContent);
            RootPanel.get(SEPARATOR).add(_maximizeContent);
            slideContentOff();
        } else if (!minimized && _maximizeContent.isAttached()) {
            RootPanel.get(SEPARATOR).remove(_maximizeContent);
            RootPanel.get(SEPARATOR).add(_minimizeContent);
            slideContentOn();
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
        if (_mheader != null) {
            _mheader.selectTab(null);
        }
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
        RootPanel.get(CLIENT).clear();
        RootPanel.get(CLIENT).setWidth(Math.max(Window.getClientWidth() - CONTENT_WIDTH, 0) + "px");
        RootPanel.get(CONTENT).setWidth(CONTENT_WIDTH + "px");
        if (_bar != null) {
            _bar.setCloseVisible(false);
        }

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
            slideContentOff();
            RootPanel.get(SEPARATOR).remove(_minimizeContent);
            History.newItem(_closeToken);
        }

        _contlist = null;
        _scroller = null;
        _bar = null;
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
            if (CShell.getMemberId() == 0) {
                RootPanel.get(HEADER).add(_gheader);
            } else {
                RootPanel.get(HEADER).add(_mheader);
            }
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
     * Displays the supplied page content.
     */
    protected static void showContent (String pageId, Widget pageContent)
    {
        RootPanel.get(CONTENT).clear();
        _bar = null;

        // clear out any lingering dialogs
        clearDialog(Predicate.TRUE);

        // note that this is our current content
        _contlist = new FlowPanel();
        _contlist.setWidth("100%");
        _contlist.add(pageContent);

        if (pageId != null) {
            // select the appropriate header tab
            if (_mheader != null) {
                _mheader.selectTab(pageId);
            }
            // create our page title bar
            _bar = createTitleBar(pageId);
        }

        Widget content;
        // if we're not showing a Page.Content page or the browser is too short; don't try to do
        // our own custom scrolling, just let everything scroll
        if (windowTooShort() || pageId == null) {
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
            slideContentOn();

        } else {
            if (_bar != null) {
                RootPanel.get(CONTENT).add(_bar);
            }
            RootPanel.get(CONTENT).add(content);
            RootPanel.get(CONTENT).setWidth(CONTENT_WIDTH + "px");
        }

        int ccount = RootPanel.get(CLIENT).getWidgetCount();
        if (ccount == 0) {
            RootPanel.get(CLIENT).add(new HTML("&nbsp;"));
        }

        if (_bar != null) {
            _bar.setCloseVisible(ccount > 0);
        }
    }

    protected static boolean windowTooShort ()
    {
        return Window.getClientHeight() < (HEADER_HEIGHT + CLIENT_HEIGHT);
    }

    protected static void restoreClient ()
    {
        setContentMinimized(true);
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

    protected static TitleBar createTitleBar (String pageId)
    {
        SubNaviPanel subnavi = new SubNaviPanel();

        if (pageId.equals(Page.ME)) {
            subnavi.addLink(null, "Me", Page.ME, "");
            if (CShell.roomCount > 1) {
                subnavi.addLink(null, "My Rooms", Page.ME, "rooms");
            } else {
                subnavi.addLink(null, "My Home", Page.WORLD, "h");
            }
            subnavi.addLink(null, "My Profile", Page.PEOPLE, "me");
            subnavi.addLink(null, "Mail", Page.MAIL, "");
            subnavi.addLink(null, "Account", Page.ME, "account");
            if (CShell.isSupport()) {
                subnavi.addLink(null, "Admin", Page.ADMIN, "");
            }

        } else if (pageId.equals(Page.PEOPLE)) {
            if (CShell.getMemberId() == 0) {
                subnavi.addLink(null, "Search", Page.PEOPLE, "");
            } else {
                subnavi.addLink(null, "My Friends", Page.PEOPLE, "");
                subnavi.addLink(null, "Invite Friends", Page.PEOPLE, "invites");
            }

        } else if (pageId.equals(Page.GAMES)) {
            subnavi.addLink(null, "Games", Page.GAMES, "");
            if (CShell.getMemberId() != 0) {
                subnavi.addLink(null, "My Trophies", Page.GAMES,
                                Args.compose("t", CShell.getMemberId()));
            }

        } else if (pageId.equals(Page.WHIRLEDS)) {
            subnavi.addLink(null, "Whirleds", Page.WHIRLEDS, "");
            if (CShell.getMemberId() != 0) {
                subnavi.addLink(null, "My Discussions", Page.WHIRLEDS, "unread");
            }

        } else if (pageId.equals(Page.SHOP)) {
            subnavi.addLink(null, "Shop", Page.SHOP, "");
            for (int ii = 0; ii < Item.TYPES.length; ii++) {
                byte type = Item.TYPES[ii];
                String tpath = Item.getDefaultThumbnailMediaFor(type).getMediaPath();
                String tname = CShell.dmsgs.getString("pItemType" + type);
                Image icon = subnavi.addImageLink(tpath, tname, Page.SHOP, ""+type);
                icon.setWidth("15px"); // shrinky!
                icon.setHeight("15px"); // shrinky!
            }

        } else if (pageId.equals(Page.HELP)) {
            subnavi.addLink(null, "Help", Page.HELP, "");
        }

        return new TitleBar(pageId, Page.getDefaultTitle(pageId), subnavi);
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

    protected static void slideContentOff ()
    {
        RootPanel.get(CONTENT).clear();
        WorldClient.setMinimized(false);
        _bar.setCloseVisible(false);
        RootPanel.get(CONTENT).setWidth("0px");
        RootPanel.get(CLIENT).setWidth((Window.getClientWidth() - SEPARATOR_WIDTH) + "px");
    }

    protected static void slideContentOn ()
    {
        // the flash client needs to be notified that the size change it is about to receive is a 
        // minimization, not a browser size change before it actually gets resized.
        WorldClient.setMinimized(true);
        RootPanel.get(CONTENT).clear();
        RootPanel.get(CONTENT).add(_bar);
        if (_scroller == null) {
            RootPanel.get(CONTENT).add(_contlist);
        } else {
            RootPanel.get(CONTENT).add(_scroller);
        }
        RootPanel.get(CONTENT).setWidth(CONTENT_WIDTH + "px");
        int availWidth = Window.getClientWidth() - SEPARATOR_WIDTH;
        RootPanel.get(CLIENT).setWidth(Math.max(availWidth - CONTENT_WIDTH, 0) + "px");
        _bar.setCloseVisible(true);
    }

    protected static class Dialog extends SmartTable
    {
        public Dialog (String title, Widget content) {
            super("pageDialog", 0, 0);
            setText(0, 0, title, 1, "DialogTitle");
            setWidget(0, 1, MsoyUI.createActionLabel("", "CloseBox", new ClickListener() {
                public void onClick (Widget sender) {
                    Frame.clearDialog(getContent());
                }
            }), 1, "Close");
            setWidget(1, 0, content, 2, null);
            setWidget(2, 0, WidgetUtil.makeShim(5, 5), 2, null);
        }

        public Widget getContent () {
            return getWidget(1, 0);
        }
    }

    protected static class SubNaviPanel extends FlowPanel
    {
        public void addLink (Image icon, String label, String page, String args) {
            addSeparator();
            if (icon != null) {
                add(icon);
            }
            add(Application.createLink(label, page, args));
        }

        public Image addImageLink (String path, String tip, final String page, final String args) {
            addSeparator();
            Image icon = MsoyUI.createActionImage(path, new ClickListener() {
                public void onClick (Widget sender) {
                    Application.go(page, args);
                }
            });
            icon.setTitle(tip);
            add(icon);
            return icon;
        }

        protected void addSeparator () {
            if (getWidgetCount() > 0) {
                HTML seppy = new HTML("&nbsp;&nbsp;|&nbsp;&nbsp;");
                seppy.addStyleName("inline");
                add(seppy);
            }
        }
    }

    protected static class TitleBar extends SmartTable
    {
        public TitleBar (String pageId, String title, Widget subnavi) {
            super("pageTitle", 0, 0);
            addStyleName("pageTitle" + pageId.toUpperCase().substring(0, 1) + pageId.substring(1));

            setWidget(0, 0, subnavi, 1, "SubNavi");
            setText(0, 1, _deftitle = title, 1, "Title");

            _closeBox = MsoyUI.createActionLabel("", "CloseBox", new ClickListener() {
                public void onClick (Widget sender) {
                    closeContent();
                }
            });
            setWidget(0, 2, _closeBox, 1, "Close");
            setCloseVisible(false);
        }

        public void setTitle (String title) {
            setText(0, 1, title == null ? _deftitle : title);
        }

        public void setCloseVisible (boolean visible) {
            _closeBox.setVisible(visible);
        }

        protected String _deftitle;
        protected Label _closeBox;
    }

    protected static abstract class Header extends SmartTable
        implements ClickListener
    {
        public Header () {
            super("frameHeader", 0, 0);
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

        // from Header
        public void onClick (Widget sender) {
            History.newItem(""); // go to the main page
        }
    }

    protected static class MemberHeader extends Header
    {
        public MemberHeader () {
            int col = 1;
            addButton(col++, Page.ME, CShell.cmsgs.menuMe(), _images.me(), _images.ome(),
                      _images.sme());
            addButton(col++, Page.PEOPLE, CShell.cmsgs.menuFriends(), _images.friends(),
                      _images.ofriends(), _images.sfriends());
            addButton(col++, Page.GAMES, CShell.cmsgs.menuGames(), _images.games(), _images.ogames(),
                      _images.sgames());
            addButton(col++, Page.WHIRLEDS, CShell.cmsgs.menuWorlds(), _images.worlds(),
                      _images.oworlds(), _images.sworlds());
            addButton(col++, Page.SHOP, CShell.cmsgs.menuShop(), _images.shop(), _images.oshop(),
                      _images.sshop());
            addButton(col++, Page.HELP, CShell.cmsgs.menuHelp(), _images.help(), _images.ohelp(),
                      _images.shelp());

            getFlexCellFormatter().setHorizontalAlignment(0, col, HasAlignment.ALIGN_RIGHT);
            setWidget(0, col++, CShell.app.getStatusPanel(), 1, "Right");
        }

        public void selectTab (String pageId) {
            for (int ii = 0; ii < _buttons.size(); ii++) {
                NaviButton button = (NaviButton)_buttons.get(ii);
                button.setSelected(button.pageId.equals(pageId));
            }
        }

        // from Header
        public void onClick (Widget sender) {
            // if the world is open, close the content, otherwise go home
            if (!Frame.closeContent()) {
                Application.go(Page.WORLD, "h");
            }
        }

        protected void addButton (int col, String pageId, String text, AbstractImagePrototype up,
                                  AbstractImagePrototype over, AbstractImagePrototype down) {
            NaviButton button = new NaviButton(pageId, text, up, over, down);
            setWidget(0, col, button);
            _buttons.add(button);
        }

        protected ArrayList _buttons = new ArrayList();
    }

    protected static class NaviButton extends SimplePanel
    {
        public final String pageId;

        public NaviButton (String page, String text, AbstractImagePrototype up,
                           AbstractImagePrototype over, AbstractImagePrototype down) {
            setStyleName("Button");
            pageId = page;

            _upImage = up.createImage();
            _upImage.addStyleName("actionLabel");
            _upImage.addMouseListener(new MouseListenerAdapter() {
                public void onMouseEnter (Widget sender) {
                    setWidget(_overImage);
                }
            });

            _overImage = over.createImage();
            _overImage.addStyleName("actionLabel");
            _overImage.addMouseListener(new MouseListenerAdapter() {
                public void onMouseLeave (Widget sender) {
                    setWidget(_upImage);
                }
            });
            ClickListener go = new ClickListener() {
                public void onClick (Widget sender) {
                    Application.go(pageId, "");
                }
            };
            _overImage.addClickListener(go);

            _downImage = down.createImage();
            _downImage.addStyleName("actionLabel");
            _downImage.addClickListener(go);

            setWidget(_upImage);
        }

        public void setSelected (boolean selected)
        {
            setWidget(selected ? _downImage : _upImage);
        }

        protected Image _upImage, _overImage, _downImage;
    }

    protected static WindowResizeListener _resizer = new WindowResizeListener() {
        public void onWindowResized (int width, int height) {
            if (_scroller != null) {
                _scroller.setHeight((Window.getClientHeight() - HEADER_HEIGHT) + "px");
            }
        }
    };

    protected static MemberHeader _mheader;
    protected static GuestHeader _gheader;
    protected static String _closeToken;

    protected static TitleBar _bar;

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
