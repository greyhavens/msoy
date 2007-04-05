//
// $Id$

package client.shell;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;

import com.threerings.msoy.web.data.WebCreds;

/**
 * Handles some standard services for a top-level MetaSOY page.
 */
public abstract class Page
{
    /** Used to dynamically create the appropriate page when we are loaded. */
    public static interface Creator {
        public Page createPage ();
    }

    /** Indicates whether we are currently displaying a Flash applet over the parts of the page
     * where popups might show up. */
    public static boolean displayingFlash = false;

    /** Indicates whether we are currently displaying a Java applet over the parts of the page
     * where popups might show up. */
    public static boolean displayingJava = false;

    /**
     * Returns the current page arguments extracted from the history token.
     */
    public static String getPageArgs ()
    {
        String token = History.getToken();
        int semidx = token.indexOf("-");
        return (semidx == -1) ? "" : token.substring(semidx+1);
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
     * Application#createLink} links within tihs page.
     */
    public void onHistoryChanged (String token)
    {
    }

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
     * Clears out any existing content and sets the specified widget as the main page content.
     */
    protected void setContent (Widget content)
    {
        setContent(content, false, false);
    }

    /**
     * Clears out any existing content and sets the specified widget as the main page content.
     */
    protected void setContent (Widget content, boolean contentIsFlash, boolean contentIsJava)
    {
        WorldClient.minimize();
        displayingFlash = contentIsFlash;
        displayingJava = contentIsJava;
        RootPanel.get("content").clear();
        // clear out any content height overrides
        setContentStretchHeight(false);
        // now set our content
        if (_content == null) {
            createContentContainer();
        }
        _content.setWidget(1, 0, content);
        RootPanel.get("content").add(_content);
        // if there isn't anything in the tabs area, the name ends up centering over the whole
        // display.
        if (_content.getWidget(0, 1) == null) {
            setPageTabs(new HTML("&nbsp;"));
        }
    }

    protected void setContentStretchHeight (boolean stretch)
    {
        String height = stretch ? "99%" : ""; // fucking browsers
        RootPanel.get("ctable").setHeight(height);
        RootPanel.get("content").setHeight(height);
    }

    protected void createContentContainer () 
    {
        _content = new FlexTable();
        _content.setCellPadding(0);
        _content.setCellSpacing(0);
        _content.setWidth("100%");
        _content.getFlexCellFormatter().setStyleName(0, 0, "pageHeaderTitle");
        _content.getFlexCellFormatter().setStyleName(0, 1, "pageHeaderContent");
        _content.getFlexCellFormatter().setStyleName(0, 2, "pageHeaderClose");
        _content.getFlexCellFormatter().setColSpan(1, 0, 3);
    }

    protected void setPageTitle (String title)
    {
        if (_content == null) {
            createContentContainer();
        }
        // TODO we should probably change the HTML <title> here as well
        _content.setWidget(0, 0, new Label(title));
    }

    protected void setPageTabs (Widget tabs) 
    {
        if (_content == null) {
            createContentContainer();
        }
        _content.setWidget(0, 1, tabs);
    }

    /**
     * Called when we the player logs on while viewing this page. The default implementation
     * redisplays the current page with the current args (by calling {@link #onHistoryChanged}.
     */
    protected void didLogon (WebCreds creds)
    {
        WorldClient.didLogon(creds);
        onHistoryChanged(getPageArgs());
    }

    /**
     * Called when the player logs off while viewing this page. The default implementation
     * redisplays the current page with the current args (by calling {@link #onHistoryChanged}.
     */
    protected void didLogoff ()
    {
        WorldClient.didLogoff();
        onHistoryChanged(getPageArgs());
    }

    protected FlexTable _content;
}
