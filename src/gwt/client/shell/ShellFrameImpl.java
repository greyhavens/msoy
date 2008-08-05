//
// $Id$

package client.shell;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.WindowResizeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.msoy.web.data.SessionData;

import client.ui.MsoyUI;
import client.util.FlashClients;
import client.util.Link;

/**
 * The frame wraps the top-level page HTML and handles displaying the navigation, the page content,
 * and the various clients.
 */
public class ShellFrameImpl
    implements Frame, WorldClient.Container, Session.Observer, WindowResizeListener
{
    /**
     * Called by the Application to initialize us once in the lifetime of the app.
     */
    public ShellFrameImpl ()
    {
        // listen for window resize so that we can adjust the size of our scrollers
        Window.addWindowResizeListener(this);

        // set up the callbackd that our flash clients can call
        configureCallbacks(this);

        // listen for logon/logff
        Session.addObserver(this);

        // load up various JavaScript dependencies
        for (int ii = 0; ii < JS_DEPENDS.length; ii += 2) {
            Element e = DOM.getElementById(JS_DEPENDS[ii]);
            if (e != null) {
                DOM.setElementAttribute(e, "src", JS_DEPENDS[ii+1]);
            }
        }

        // create our header, dialog and popup
        _header = new FrameHeader(new ClickListener() {
            public void onClick (Widget sender) {
                if (_closeToken != null) {
                    closeContent();
                } else if (CShell.isGuest()) {
                    History.newItem("");
                } else {
                    Link.go(Pages.WORLD, "m" + CShell.getMemberId());
                }
            }
        });
        _dialog = new Dialog();
        _popup = new PopupDialog();

        // default to scrolling off.  In the rare case where a page wants scrolling, it gets enabled
        // explicitly.
        Window.enableScrolling(false);

        // clear out the loading HTML so we can display a browser warning or load Whirled
        DOM.setInnerHTML(RootPanel.get(LOADING_AND_TESTS).getElement(), "");

        // If the browser is unsupported, hide the page (still being built) and show a warning.
        ClickListener continueClicked = new ClickListener() {
            public void onClick (Widget widget) {
                // close the warning and show the page if the visitor choose to continue
                RootPanel.get(LOADING_AND_TESTS).clear();
                RootPanel.get(LOADING_AND_TESTS).setVisible(false);
                RootPanel.get(SITE_CONTAINER).setVisible(true);
            }
        };
        Widget warningDialog = BrowserTest.getWarningDialog(continueClicked);
        if (warningDialog != null) {
            RootPanel.get(SITE_CONTAINER).setVisible(false);
            RootPanel.get(LOADING_AND_TESTS).add(warningDialog);
        } else {
            RootPanel.get(LOADING_AND_TESTS).clear();
            RootPanel.get(LOADING_AND_TESTS).setVisible(false);
        }
    }

    // from interface Frame
    public void setTitle (String title)
    {
        if (_bar != null && title != null) {
            _bar.setTitle(title);
        }
        Window.setTitle(title == null ? _cmsgs.bareTitle() : _cmsgs.windowTitle(title));
    }

    // from interface Frame
    public void navigateTo (String token)
    {
        if (!token.equals(History.getToken())) {
            History.newItem(token);
        }
    }

    // from interface Frame
    public void navigateReplace (String token)
    {
        History.back();
        History.newItem(token);
    }

    // frome interface Frame
    public void displayWorldClient (String args, String closeToken)
    {
        WorldClient.displayFlash(args, closeToken, this);
    }

    // from interface Frame
    public void closeClient ()
    {
        WorldClient.clientWillClose();
        _closeToken = null;
        _cscroller = null;

        RootPanel.get(CLIENT).clear();
        RootPanel.get(CLIENT).setWidth(Math.max(Window.getClientWidth() - CONTENT_WIDTH, 0) + "px");
        RootPanel.get(CONTENT).setWidth(CONTENT_WIDTH + "px");
        RootPanel.get(CONTENT).setVisible(true);
        if (_bar != null) {
            _bar.setCloseVisible(false);
        }

        // if we're on a "world" page, go to a landing page
        String curToken = History.getToken();
        if (curToken.startsWith(Pages.WORLD.getPath())) {
            // if we were in a game, go to the games page, otherwise go to me
            Link.go(curToken.indexOf("game") == -1 ? Pages.ME : Pages.GAMES, "");
        }
    }

    // from interface Frame
    public void closeContent ()
    {
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
        if (_closeToken != null) {
            History.newItem(_closeToken);
        }
    }

    // from interface Frame
    public void setHeaderVisible (boolean visible)
    {
        RootPanel.get(HEADER).remove(_header);
        if (visible) {
            RootPanel.get(HEADER).add(_header);
        }
    }

    // from interface Frame
    public void ensureVisible (Widget widget)
    {
        if (_scroller != null) {
            _scroller.ensureVisible(widget);
        }
    }

    // from interface Frame
    public void showDialog (String title, Widget dialog)
    {
        // remove any existing content
        clearDialog();

        // update the dialog content and add it
        _dialog.update(title, dialog);
        RootPanel.get(HEADER).add(_dialog); // TODO: animate this sliding down
    }

    // from interface Frame
    public void showPopupDialog (String title, Widget dialog)
    {
        _popup.setVisible(false);
        _popup.update(title, dialog);
        _popup.setVisible(true);
        _popup.center();
    }

    // from interface Frame
    public void clearDialog ()
    {
        RootPanel.get(HEADER).remove(_dialog);
    }

    // from interface Frame
    public void showContent (Pages page, Widget pageContent)
    {
        RootPanel.get(CONTENT).clear();
        _bar = null;

        // clear out any lingering dialog content
        clearDialog();

        // note that this is our current content
        _contlist = new FlowPanel();
        _contlist.setHeight("100%");
        _contlist.add(pageContent);

        if (page!= null) {
            // select the appropriate header tab
            _header.selectTab(page.getTab());
            // create our page title bar
            _bar = TitleBar.create(page.getTab(), new ClickListener() {
                public void onClick (Widget sender) {
                    closeContent();
                }
            });
        }

        // let the client know it about to be minimized
        WorldClient.setMinimized(true);
        int clientWidth = Math.max(Window.getClientWidth() - CONTENT_WIDTH, 300);
        RootPanel.get(CLIENT).setWidth(clientWidth + "px");

        // if we're not showing a Pages.Content page, don't do our custom scrolling
        Widget content;
        if (page == null) {
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

        // add our title bar if we've got one
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

//     // from interface Frame
//     public void logon (String username, String password)
//     {
//     }

    // from interface Frame
    public String md5hex (String text)
    {
        return nmd5hex(text);
    }

    // from interface WorldClient.Container
    public void setShowingClient (String closeToken)
    {
        // note the current history token so that we can restore it if needed
        _closeToken = (closeToken == null) ? History.getToken() : closeToken;

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

    // from interface WorldClient.Container
    public Panel getClientContainer ()
    {
        RootPanel.get(CLIENT).clear();
        if (Window.getClientHeight() < (HEADER_HEIGHT + CLIENT_HEIGHT)) {
            RootPanel.get(CLIENT).add(_cscroller = new ScrollPanel());
            _cscroller.setHeight((Window.getClientHeight() - NAVI_HEIGHT) + "px");
            return _cscroller;
        } else {
            _cscroller = null;
            return RootPanel.get(CLIENT);
        }
    }

    // from interface Session.Observer
    public void didLogon (SessionData data)
    {
        clearDialog();
    }

    // from interface Session.Observer
    public void didLogoff ()
    {
    }

    // from interface WindowResizeListener
    public void onWindowResized (int width, int height)
    {
        if (_scroller != null) {
            _scroller.setHeight((Window.getClientHeight() - HEADER_HEIGHT) + "px");
        }
        if (_cscroller != null) {
            _cscroller.setHeight((Window.getClientHeight() - NAVI_HEIGHT) + "px");
        }
    }

    protected void deferredCloseClient ()
    {
        DeferredCommand.addCommand(new Command() {
            public void execute () {
                closeClient();
            }
        });
    }

    /**
     * Configures top-level functions that can be called by Flash.
     */
    protected static native void configureCallbacks (ShellFrameImpl impl) /*-{
       $wnd.clearClient = function () {
            impl.@client.shell.ShellFrameImpl::deferredCloseClient()();
       };
    }-*/;

    protected static native boolean isLinux () /*-{
        return (navigator.userAgent.toLowerCase().indexOf("linux") != -1);
    }-*/;

    protected class Dialog extends SimplePanel
    {
        public Dialog () {
            init(new ClickListener() {
                public void onClick (Widget sender) {
                    clearDialog();
                }
            });
        }

        public Dialog (ClickListener closeListener) {
            init(closeListener);
        }

        protected void init(ClickListener closeListener) {
            setWidget(_innerTable = new SmartTable("pageDialog", 0, 0));

            _innerTable.setWidget(0, 1, MsoyUI.createCloseButton(closeListener), 1, "Close");
            _innerTable.getFlexCellFormatter().setHorizontalAlignment(
                1, 0, HasAlignment.ALIGN_CENTER);
            _innerTable.setWidget(2, 0, WidgetUtil.makeShim(5, 5), 2, null);
        }

        public void update(String title, Widget content) {
            _innerTable.setText(0, 0, title, 1, "DialogTitle");
            _innerTable.setWidget(1, 0, content, 2, null);
        }

        protected SmartTable _innerTable;
    }

    protected class PopupDialog extends PopupPanel
    {
        public PopupDialog () {
            super(false);
            setAnimationEnabled(true);
            setStyleName("floatingDialogBox");

            _innerDialog = new Dialog(new ClickListener() {
                public void onClick (Widget sender) {
                    setVisible(false);
                }
            });
            setWidget(_innerDialog);
        }

        public void update (String title, Widget content) {
            _innerDialog.update(title, content);
        }

        protected Dialog _innerDialog;
    }

    /** MD5 hashes the supplied text and returns the hex encoded hash value. */
    public native static String nmd5hex (String text) /*-{
        return $wnd.hex_md5(text);
    }-*/;

    protected FrameHeader _header;
    protected String _closeToken;

    protected TitleBar _bar;

    protected FlowPanel _contlist;
    protected ScrollPanel _scroller;
    protected ScrollPanel _cscroller;
    protected Dialog _dialog;
    protected PopupDialog _popup;

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);

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
    protected static final String SITE_CONTAINER = "ctable";
    protected static final String LOADING_AND_TESTS = "loadingAndTests";
}
