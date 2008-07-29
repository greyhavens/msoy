//
// $Id$

package client.shell;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.data.SessionData;
import com.threerings.msoy.web.data.WebCreds;
import com.threerings.msoy.web.data.WebIdent;

import client.util.FlashClients;
import client.util.Link;

/**
 * Handles some standard services for a top-level MetaSOY page.
 */
public abstract class Page
    implements EntryPoint
{
    /** Used to dynamically create the appropriate page when we are loaded. */
    public static interface Creator {
        Page createPage ();
    }

    // constants for our various pages
    public static final String ACCOUNT = "account";
    public static final String ADMIN = "admin";
    public static final String CREATE = "create";
    public static final String GAMES = "games";
    public static final String HELP = "help";
    public static final String MAIL = "mail";
    public static final String ME = "me";
    public static final String PEOPLE = "people";
    public static final String SHOP = "shop";
    public static final String STUFF = "stuff";
    public static final String SUPPORT = "support";
    public static final String SWIFTLY = "swiftly";
    public static final String WHIRLEDS = "whirleds";
    public static final String WORLD = "world";

    /**
     * Returns the default title for the specified page.
     */
    public static String getDefaultTitle (String pageId)
    {
        try {
            return _dmsgs.getString(pageId + "Title");
        } catch (Exception e) {
            return null;
        }
    }

    // NEW STUFF -- ignore for now

    // from interface EntryPoint
    public void onModuleLoad ()
    {
        init();

        // set up our title bar and content width
        if (getTabPageId() != null) {
            _bar = TitleBar.create(getTabPageId(), new ClickListener() {
                public void onClick (Widget sender) {
                    // TODO
                }
            });
            RootPanel.get("content").add(_bar);
            RootPanel.get("content").setWidth(Frame.CONTENT_WIDTH + "px");
        } else {
            RootPanel.get("content").setWidth("");
        }

        // wire ourselves up to the top-level frame
        if (configureCallbacks(this)) {
            // if we're running in standalone page test mode, we do a bunch of stuff
            CShell.frame = new PageFrame() {
                public void setTitle (String title) {
                    super.setTitle(title);
                    frameSetTitle(title);
                }
                public void navigateTo (String token) {
                    frameNavigateTo(token);
                }
                public void navigateReplace (String token) {
                    frameNavigateReplace(token);
                }
            };

            // obtain our current credentials from the frame
            CShell.creds = WebCreds.unflatten(frameGetWebCreds());
            CShell.ident = new WebIdent(CShell.creds.getMemberId(), CShell.creds.token);
            // TODO: level, activeInvite

            // and get our current page token from our containing frame
            setPageToken(frameGetPageToken());
            // TODO: nix the above and just call didLogon() and have that get our page token
            // properly instead of via History

        } else {
            // if we're running in standalone page test mode, we do a bunch of stuff
            CShell.frame = new PageFrame() {
                public void setTitle (String title) {
                    super.setTitle(title);
                    Window.setTitle(title == null ? _cmsgs.bareTitle() : _cmsgs.windowTitle(title));
                }
                public void navigateTo (String token) {
                    if (!token.equals(History.getToken())) {
                        History.newItem(token);
                    }
                }
                public void navigateReplace (String token) {
                    History.back();
                    History.newItem(token);
                }
            };

            final HistoryListener listener = new HistoryListener() {
                public void onHistoryChanged (String token) {
                    // this is only called when we're in single page test mode, so we assume we're
                    // staying on the same page and just pass the arguments back into ourselves
                    token = token.substring(token.indexOf("-")+1);
                    setPageToken(token);
                }
            };
            History.addHistoryListener(listener);

            Session.addObserver(new Session.Observer() {
                public void didLogon (SessionData data) {
                    listener.onHistoryChanged(""); // TODO get our page token from the frame
                }
                public void didLogoff () {
                    listener.onHistoryChanged(""); // TODO get our page token from the frame
                }
            });
            Session.validate();
        }
    }

    // END NEW STUFF

    /**
     * Called when the page is first resolved to initialize its bits.
     */
    public void init ()
    {
        // initialize our services and translations
        initContext();
    }

    /**
     * Called when the user has navigated to this page. A call will immediately follow to {@link
     * #onHistoryChanged} with the arguments passed to this page or the empty string if no
     * arguments were supplied.
     */
    public void onPageLoad ()
    {
    }

    /**
     * Called when the user navigates to this page for the first time, and when they follow {@link
     * Link.create} links within tihs page.
     */
    public abstract void onHistoryChanged (Args args);

    /**
     * Called when the user navigates away from this page to another page. Gives the page a chance
     * to shut anything down before its UI is removed from the DOM.
     */
    public void onPageUnload ()
    {
    }

    /**
     * Called when we the player logs on while viewing this page. The default implementation
     * redisplays the current page with the current args (by calling {@link #onHistoryChanged}.
     */
    public void didLogon (WebCreds creds)
    {
        History.onHistoryChanged(History.getToken());
    }

    /**
     * Called when the player logs off while viewing this page. The default implementation sends
     * the user to the landing page.
     */
    public void didLogoff ()
    {
        // TODO: move this into the frame
        // go to the landing page by hook or crook
        if (History.getToken().equals("")) {
            Link.replace("", "");
        } else {
            History.newItem("");
        }
    }

    /**
     * Returns the identifier of this page (used for navigation).
     */
    public abstract String getPageId ();

    /**
     * Called during initialization to give our entry point and derived classes a chance to
     * initialize their respective context classes.
     */
    protected void initContext ()
    {
    }

    /**
     * Returns the identifier the page whose tab should be showing when this page is showing.
     */
    protected String getTabPageId ()
    {
        return getPageId();
    }

    /**
     * Returns the content widget last configured with {@link #setContent}.
     */
    protected Widget getContent ()
    {
        return _content;
    }

    /**
     * Clears out any existing content, creates a new Flash object from the definition, and sets it
     * as the new main page content. Returns the newly-created content as a widget.
     */
    protected HTML setFlashContent (String title, String definition)
    {
        // Please note: the following is a work-around for an IE7 bug. If we create a Flash object
        // node *before* attaching it to the DOM tree, IE will silently fail to register the Flash
        // object's callback functions for access from JavaScript. To make this work, create an
        // empty node first, add it to the DOM tree, and then initialize it with the Flash object
        // definition.  Also see: WidgetUtil.embedFlashObject()
        HTML control = new HTML();
        setContent(title, control);
        control.setHTML(definition);
        return control;
    }

    /**
     * Clears out any existing content and sets the specified widget as the main page content.
     */
    protected void setContent (Widget content)
    {
        setContent(null, content, true);
    }

    /**
     * Clears out any existing content and sets the specified widget as the main page content.
     */
    protected void setContent (String title, Widget content)
    {
        setContent(title, content, true);
    }

    /**
     * Clears out any existing content and sets the specified widget as the main page content.
     */
    protected void setContent (String title, Widget content, boolean withHeader)
    {
        CShell.frame.setHeaderVisible(withHeader);
        CShell.frame.showContent(withHeader ? getTabPageId() : null, _content = content);
        CShell.frame.setTitle(title == null ? getDefaultTitle(getTabPageId()) : title);
    }

    /**
     * Called when our page token has been changed by the outer frame.
     */
    protected void setPageToken (String token)
    {
        CShell.log("Got new page token " + token);
        Args args = new Args();
        args.setToken(token);
        onHistoryChanged(args);
    }

    /**
     * Wires ourselves up to our enclosing frame.
     *
     * @return true if we're running as a subframe, false if we're running in standalone test mode.
     */
    protected static native boolean configureCallbacks (Page page) /*-{
        $wnd.top.setPageToken = function (token) {
            page.@client.shell.Page::setPageToken(Ljava/lang/String;)(token)
        };
        return $wnd != $wnd.top;
    }-*/;

    /**
     * Calls up to our containing frame to get our current credentials.
     */
    protected static native String frameGetWebCreds () /*-{
        return $wnd.top.getWebCreds();
    }-*/;

    /**
     * Calls up to our containing frame to get our current page token.
     */
    protected static native String frameGetPageToken () /*-{
        return $wnd.top.getPageToken();
    }-*/;

    /**
     * Calls up to our containing frame and sets the page title.
     */
    protected static native void frameSetTitle (String title) /*-{
        $wnd.top.setWindowTitle(title);
    }-*/;

    /**
     * Calls up to our containing frame to navigate to the specified token.
     */
    protected static native void frameNavigateTo (String token) /*-{
        $wnd.top.navigateTo(token);
    }-*/;

    /**
     * Calls up to our containing frame to replace the current page with the specified token.
     */
    protected static native void frameNavigateReplace (String token) /*-{
        $wnd.top.navigateReplace(token);
    }-*/;

    protected abstract class PageFrame implements Frame
    {
        public void setTitle (String title) {
            if (_bar != null && title != null) {
                _bar.setTitle(title);
            }
        }

        public void navigateTo (String token) {
            if (!token.equals(History.getToken())) {
                History.newItem(token);
            }
        }

        public void navigateReplace (String token) {
            History.back();
            History.newItem(token);
        }

        public void setShowingClient (String closeToken) {
        }
        public void closeClient (boolean deferred) {
        }
        public boolean closeContent () {
            return false;
        }
        public void setHeaderVisible (boolean visible) {
        }
        public void ensureVisible (Widget widget) {
        }
        public void showDialog (String title, Widget dialog) {
        }
        public void showPopupDialog (String title, Widget dialog) {
        }
        public void clearDialog () {
        }
        public Panel getClientContainer () {
            return null;
        }

        public void showContent (String pageId, Widget pageContent) {
            RootPanel contentDiv = RootPanel.get("content");
            if (_pageContent != null) {
                contentDiv.remove(_pageContent);
            }
            _pageContent = pageContent;
            if (_pageContent != null) {
                contentDiv.add(_pageContent);
            }
            if (_bar != null) {
                _bar.setCloseVisible(FlashClients.clientExists());
            }
        }

        protected Widget _pageContent;
    }

    protected TitleBar _bar;
    protected Widget _content;

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final DynamicMessages _dmsgs = GWT.create(DynamicMessages.class);
}
