//
// $Id$

package client.shell;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.data.SessionData;
import com.threerings.msoy.web.data.WebCreds;

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

        // TODO: check whether we're running in a frame or if we're in single page test mode, only
        // do the remaining things if we're in single page test mode

        // TEMP: set up a frame
        CShell.frame = new Frame() {
            /* Frame () */ {
                // set up our title bar and content width
                if (getTabPageId() != null) {
                    TitleBar bar = TitleBar.create(getTabPageId(), new ClickListener() {
                        public void onClick (Widget sender) {
                            // TODO
                        }
                    });
                    RootPanel.get("content").add(bar);
                    RootPanel.get("content").setWidth(CONTENT_WIDTH + "px");
                    _bar = bar; // have to do this here because we're not in a real constructor
                } else {
                    RootPanel.get("content").setWidth("");
                }
            }

            public void setTitle (String title) {
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

            protected TitleBar _bar;
            protected Widget _pageContent;
        };

        final HistoryListener listener = new HistoryListener() {
            public void onHistoryChanged (String token) {
                // this is only called when we're in single page test mode, so we assume we're
                // staying on the same page and just pass the arguments back into ourselves
                token = token.substring(token.indexOf("-")+1);
                Args args = new Args();
                args.setToken(token);
                Page.this.onHistoryChanged(args);
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

    protected Widget _content;
    protected static final DynamicMessages _dmsgs = GWT.create(DynamicMessages.class);
}
