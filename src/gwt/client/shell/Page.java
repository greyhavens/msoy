//
// $Id$

package client.shell;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.data.WebCreds;

import client.util.Link;

/**
 * Handles some standard services for a top-level MetaSOY page.
 */
public abstract class Page
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
            return CShell.dmsgs.getString(pageId + "Title");
        } catch (Exception e) {
            return null;
        }
    }

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
     * Called during initialization to give our entry point and derived classes a chance to
     * initialize their respective context classes.
     */
    protected void initContext ()
    {
    }

    /**
     * Returns the identifier of this page (used for navigation).
     */
    protected abstract String getPageId ();

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
        Frame.setHeaderVisible(withHeader);
        Frame.showContent(withHeader ? getTabPageId() : null, _content = content);
        Frame.setTitle(title == null ? getDefaultTitle(getTabPageId()) : title);
    }

    /**
     * Called when we the player logs on while viewing this page. The default implementation
     * redisplays the current page with the current args (by calling {@link #onHistoryChanged}.
     */
    protected void didLogon (WebCreds creds)
    {
        History.onHistoryChanged(History.getToken());
    }

    /**
     * Called when the player logs off while viewing this page. The default implementation sends
     * the user to the landing page.
     */
    protected void didLogoff ()
    {
        // go to the landing page by hook or crook
        if (History.getToken().equals("")) {
            CShell.app.onHistoryChanged("");
        } else {
            History.newItem("");
        }
    }

    protected Widget _content;
}
