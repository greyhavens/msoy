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
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.WindowResizeListener;

import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.MouseListenerAdapter;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.gwt.util.Predicate;

import client.images.navi.NaviImages;
import client.util.FlashClients;
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

    /** The offset of the content close button, from the left edge of the separator bar. */
    public static final int CLOSE_BUTTON_OFFSET = -16;

    /**
     * Called by the Application to initialize us once in the lifetime of the app.
     */
    public static void init ()
    {
        // if we're tall enough, handle scrolling ourselves
        Window.enableScrolling(windowTooShort());
        Window.addWindowResizeListener(_resizer);

        // set up the callbackd that our flash clients can call
        configureCallbacks();

        // load up various JavaScript dependencies
        for (int ii = 0; ii < JS_DEPENDS.length; ii += 2) {
            Element e = DOM.getElementById(JS_DEPENDS[ii]);
            if (e != null) {
                DOM.setAttribute(e, "src", JS_DEPENDS[ii+1]);
            }
        }

        // create our header
        _header = new Header();

        // clear out the loading HTML because we're about to show the Whirled
        DOM.setInnerHTML(RootPanel.get(HEADER).getElement(), "");
    }

    /**
     * Called when the user logs on.
     */
    public static void didLogon ()
    {
        _header.didLogon();

        // clear out the logon dialog
        clearDialog(new Predicate() {
            public boolean isMatch (Object o) {
                return (o instanceof LogonPanel);
            }
        });
    }

    /**
     * Called when the user logs off.
     */
    public static void didLogoff ()
    {
        _header.didLogoff();
    }

    /**
     * Sets the title of the browser window and the page.
     */
    public static void setTitle (String title)
    {
        if (_bar != null && title != null) {
            _bar.setTitle(title);
        }
        Window.setTitle(title == null ? CShell.cmsgs.bareTitle() : CShell.cmsgs.windowTitle(title));
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
        RootPanel.get(CONTENT).setVisible(false);

        // have the client take up all the space
        RootPanel.get(CLIENT).setWidth("100%");

        // make sure the header is showing as we always want the header above the client
        setHeaderVisible(true);
        _header.selectTab(null);
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
        RootPanel.get(CLIENT).clear();
        RootPanel.get(CLIENT).setWidth(Math.max(Window.getClientWidth() - CONTENT_WIDTH, 0) + "px");
        RootPanel.get(CONTENT).setWidth(CONTENT_WIDTH + "px");
        RootPanel.get(CONTENT).setVisible(true);
        if (_bar != null) {
            _bar.setCloseVisible(false);
        }

        // if we're on a "world" page, go to a landing page
        String curToken = History.getToken();
        if (curToken.startsWith(Page.WORLD)) {
            // if we were in a game, go to the games page, otherwise go to me
            Application.go(curToken.indexOf("game") == -1 ? Page.ME : Page.GAMES, "");
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

        // let the Flash client know that it's being unminimized
        WorldClient.setMinimized(false);

        // restore the client to the full glorious browser width
        RootPanel.get(CONTENT).clear();
        RootPanel.get(CONTENT).setWidth("0px");
        RootPanel.get(CONTENT).setVisible(false);
        RootPanel.get(CLIENT).setWidth("100%");

        // clear out our bits
        _contlist = null;
        _scroller = null;
        _bar = null;

        // restore the client's URL
        History.newItem(_closeToken);

        return true;
    }

    /**
     * Shows or hides the navigation header as desired.
     */
    public static void setHeaderVisible (boolean visible)
    {
        RootPanel.get(HEADER).remove(_header);
        if (visible) {
            RootPanel.get(HEADER).add(_header);
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
        _contlist.setHeight("100%");
        _contlist.add(pageContent);

        if (pageId != null) {
            // select the appropriate header tab
            _header.selectTab(pageId);
            // create our page title bar
            _bar = createTitleBar(pageId);
        }

        // let the client know it about to be minimized
        WorldClient.setMinimized(true);
        int clientWidth = Math.max(Window.getClientWidth() - CONTENT_WIDTH, 300);
        RootPanel.get(CLIENT).setWidth(clientWidth + "px");

        // if we're not showing a Page.Content page or the browser is too short; don't try to do
        // our own custom scrolling, just let everything scroll
        Widget content;
        if (windowTooShort() || pageId == null) {
            content = _contlist;
            Window.enableScrolling(true);
        } else {
            content = (_scroller = new ScrollPanel(_contlist));
            // this works around a pesky IE bug wherein absolutely positioned things inside an
            // overflow: auto (or scroll) element are le booched
            DOM.setStyleAttribute(_scroller.getElement(), "position", "relative");
            _scroller.setHeight((Window.getClientHeight() - HEADER_HEIGHT) + "px");
            Window.enableScrolling(false);
        }

        // add our title bard if we've got one
        if (_bar != null) {
            RootPanel.get(CONTENT).add(_bar);
        }

        // stuff the content into the page and size it properly
        RootPanel.get(CONTENT).add(content);
        RootPanel.get(CONTENT).setWidth(CONTENT_WIDTH + "px");
        RootPanel.get(CONTENT).setVisible(true);

        int ccount = RootPanel.get(CLIENT).getWidgetCount();
        if (ccount == 0) {
            RootPanel.get(CLIENT).add(new HTML("&nbsp;"));
        }
        if (_bar != null) {
            _bar.setCloseVisible(FlashClients.clientExists());
        }
    }

    protected static boolean windowTooShort ()
    {
        return Window.getClientHeight() < (HEADER_HEIGHT + CLIENT_HEIGHT);
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
        int memberId = CShell.getMemberId();

        if (pageId.equals(Page.ME)) {
            subnavi.addLink(null, "Me", Page.ME, "");
            subnavi.addImageLink("/images/me/menu_home.png", "My Home", Page.WORLD,
                "m" + memberId);
            subnavi.addLink(null, "My Rooms", Page.ME, "rooms");
            subnavi.addLink(null, "My Profile", Page.PEOPLE, "" + memberId);
            subnavi.addLink(null, "Mail", Page.MAIL, "");
            subnavi.addLink(null, "Account", Page.ME, "account");
            if (CShell.isSupport()) {
                subnavi.addLink(null, "Admin", Page.ADMIN, "");
            }

        } else if (pageId.equals(Page.PEOPLE)) {
            if (memberId == 0) {
                subnavi.addLink(null, "Search", Page.PEOPLE, "");
            } else {
                subnavi.addLink(null, "My Friends", Page.PEOPLE, "");
                subnavi.addLink(null, "Invite Friends", Page.PEOPLE, "invites");
            }

        } else if (pageId.equals(Page.GAMES)) {
            subnavi.addLink(null, "Games", Page.GAMES, "");
            if (memberId != 0) {
                subnavi.addLink(null, "My Trophies", Page.GAMES,
                                Args.compose("t", CShell.getMemberId()));
            }
            subnavi.addLink(null, "All Games", Page.GAMES, "g");

        } else if (pageId.equals(Page.WHIRLEDS)) {
            subnavi.addLink(null, "Whirleds", Page.WHIRLEDS, "");
            if (memberId != 0) {
                subnavi.addLink(null, "My Discussions", Page.WHIRLEDS, "unread");
                if (CShell.isAdmin()) {
                    subnavi.addLink(null, "Issues", Page.WHIRLEDS, "b");
                    subnavi.addLink(null, "My Issues", Page.WHIRLEDS, "owned");
                }
            }

        } else if (pageId.equals(Page.SHOP)) {
            subnavi.addLink(null, "Shop", Page.SHOP, "");

        } else if (pageId.equals(Page.HELP)) {
            subnavi.addLink(null, "Help", Page.HELP, "");
            if (CShell.isSupport()) {
                subnavi.addLink(null, "Admin", Page.SUPPORT, "admin");
            }
        }

        return new TitleBar(pageId, Page.getDefaultTitle(pageId), subnavi);
    }

    /**
     * Configures top-level functions that can be called by Flash.
     */
    protected static native void configureCallbacks () /*-{
       $wnd.restoreClient = function () {
            @client.shell.Frame::closeContent()();
       };
       $wnd.clearClient = function () {
            @client.shell.Frame::closeClient(Z)(true);
       };
    }-*/;

    protected static native boolean isLinux () /*-{
        return (navigator.userAgent.toLowerCase().indexOf("linux") != -1);
    }-*/;

    protected static class Dialog extends SmartTable
    {
        public Dialog (String title, Widget content) {
            super("pageDialog", 0, 0);
            setText(0, 0, title, 1, "DialogTitle");
            setWidget(0, 1, MsoyUI.createCloseButton(new ClickListener() {
                public void onClick (Widget sender) {
                    Frame.clearDialog(getContent());
                }
            }), 1, "Close");
            setWidget(1, 0, content, 2, null);
            getFlexCellFormatter().setHorizontalAlignment(1, 0, HasAlignment.ALIGN_CENTER);
            setWidget(2, 0, WidgetUtil.makeShim(5, 5), 2, null);
        }

        public Widget getContent () {
            return getWidget(1, 0);
        }
    }

    protected static class SubNaviPanel extends FlowPanel
    {
        public void addLink (String iconPath, String label, final String page, final String args) {
            addSeparator();
            if (iconPath != null) {
                add(MsoyUI.createActionImage(iconPath, new ClickListener() {
                    public void onClick (Widget sender) {
                        Application.go(page, args);
                    }
                }));
                add(new HTML("&nbsp;"));
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
                add(new HTML("&nbsp;&nbsp;|&nbsp;&nbsp;"));
            }
        }
    }

    protected static class TitleBar extends SmartTable
    {
        public TitleBar (String pageId, String title, SubNaviPanel subnavi) {
            super("pageTitle", 0, 0);

            setWidget(0, 0, createImage(pageId), 3, null);
            setText(1, 0, _deftitle = title, 1, "Title");
            setWidget(1, 1, _subnavi = subnavi, 1, "SubNavi");
            getFlexCellFormatter().setVerticalAlignment(1, 1, HasAlignment.ALIGN_BOTTOM);

            _closeBox = MsoyUI.createActionImage("/images/ui/close.png", new ClickListener() {
                public void onClick (Widget sender) {
                    closeContent();
                }
            });
            _closeBox.addStyleName("Close");
            _closeShim = MsoyUI.createHTML("&nbsp;", "Close");
            setCloseVisible(false);
        }

        public void setTitle (String title) {
            setText(1, 0, title == null ? _deftitle : title);
        }

        public void setCloseVisible (boolean visible) {
            _subnavi.remove(_closeBox);
            _subnavi.remove(_closeShim);
            if (visible) {
                _subnavi.add(_closeBox);
            } else {
                _subnavi.add(_closeShim);
            }
        }

        protected Image createImage (String page) {
            String id = (page.equals(Page.ME) || page.equals(Page.PEOPLE) ||
                         page.equals(Page.GAMES) || page.equals(Page.WHIRLEDS) ||
                         page.equals(Page.SHOP) || page.equals(Page.HELP)) ? page : "solid";
            return new Image("/images/header/" + id + "_cap.png");
        }

        protected String _deftitle;
        protected SubNaviPanel _subnavi;
        protected Widget _closeBox, _closeShim;
    }

    protected static class Header extends SmartTable
        implements ClickListener
    {
        public Header () {
            super("frameHeader", 0, 0);
            setWidth("100%");
            int col = 0;

            String lpath = "/images/header/header_logo.png";
            setWidget(col++, 0, MsoyUI.createActionImage(lpath, this), 1, "Logo");
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
            _statusCol = col;
        }

        public void didLogon () {
            getFlexCellFormatter().setHorizontalAlignment(0, _statusCol, HasAlignment.ALIGN_RIGHT);
            getFlexCellFormatter().setVerticalAlignment(0, _statusCol, HasAlignment.ALIGN_BOTTOM);
            setWidget(0, _statusCol, CShell.app.getStatusPanel(), 1, "Right");
        }

        public void didLogoff () {
            getFlexCellFormatter().setHorizontalAlignment(0, _statusCol, HasAlignment.ALIGN_CENTER);
            getFlexCellFormatter().setVerticalAlignment(0, _statusCol, HasAlignment.ALIGN_TOP);
            setWidget(0, _statusCol, new SignOrLogonPanel(), 1, "Right");
        }

        public void selectTab (String pageId) {
            for (int ii = 0; ii < _buttons.size(); ii++) {
                NaviButton button = (NaviButton)_buttons.get(ii);
                button.setSelected(button.pageId.equals(pageId));
            }
        }

        // from Header
        public void onClick (Widget sender) {
            if (closeContent()) {
                // peachy, nothing else to do
            } else {
                int memberId = CShell.getMemberId();
                if (memberId != 0) {
                    Application.go(Page.WORLD, "m" + memberId);
                } else {
                    History.newItem("");
                }
            }
        }

        protected void addButton (int col, String pageId, String text, AbstractImagePrototype up,
                                  AbstractImagePrototype over, AbstractImagePrototype down) {
            NaviButton button = new NaviButton(pageId, text, up, over, down);
            setWidget(0, col, button);
            _buttons.add(button);
        }

        protected int _statusCol;
        protected ArrayList _buttons = new ArrayList();
    }

    protected static class SignOrLogonPanel extends SmartTable
    {
        public SignOrLogonPanel () {
            super(0, 0);
            PushButton signup = new PushButton(
                CShell.cmsgs.headerSignup(),
                Application.createLinkListener(Page.ACCOUNT, "create"));
            signup.setStyleName("SignupButton");
            signup.addStyleName("Button");
            setWidget(0, 0, signup);
            getFlexCellFormatter().setVerticalAlignment(0, 0, HasAlignment.ALIGN_TOP);
            setWidget(0, 1, WidgetUtil.makeShim(10, 10));
            PushButton logon = new PushButton(CShell.cmsgs.headerLogon(), new ClickListener() {
                public void onClick (Widget sender) {
                    setWidget(0, 2, new LogonPanel(true));
                }
            });
            logon.setStyleName("LogonButton");
            logon.addStyleName("Button");
            setWidget(0, 2, logon);
            getFlexCellFormatter().setVerticalAlignment(0, 2, HasAlignment.ALIGN_TOP);
        }
    }

    protected static class NaviButton extends SimplePanel
    {
        public final String pageId;

        public NaviButton (String page, String text, AbstractImagePrototype up,
                           AbstractImagePrototype over, AbstractImagePrototype down) {
            setStyleName("NaviButton");
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
                    // if a guest clicks on "me", send them to create account
                    if (pageId.equals(Page.ME) && CShell.getMemberId() == 0) {
                        Application.go(Page.ACCOUNT, "create");
                    } else {
                        Application.go(pageId, "");
                    }
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

    protected static Header _header;
    protected static String _closeToken;

    protected static TitleBar _bar;

    protected static FlowPanel _contlist;
    protected static ScrollPanel _scroller;

    /** Our navigation menu images. */
    protected static NaviImages _images = (NaviImages)GWT.create(NaviImages.class);

    /** Enumerates our Javascript dependencies. */
    protected static final String[] JS_DEPENDS = {
        "swfobject", "/js/swfobject.js",
        "md5", "/js/md5.js",
        "recaptcha", "http://api.recaptcha.net/js/recaptcha_ajax.js",
        "googanal", "http://www.google-analytics.com/ga.js",
    };

    // constants for our top-level elements
    protected static final String HEADER = "header";
    protected static final String CONTENT = "content";
    protected static final String CLIENT = "client";
}
