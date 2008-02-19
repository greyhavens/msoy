//
// $Id$

package client.shell;

import java.util.ArrayList;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.web.data.WebCreds;

import client.util.MsoyUI;

/**
 * Handles some standard services for a top-level MetaSOY page.
 */
public abstract class Page
{
    /** Used to dynamically create the appropriate page when we are loaded. */
    public static interface Creator {
        public Page createPage ();
    }

    // constants for our various pages
    public static final String ACCOUNT = "account";
    public static final String ADMIN = "admin";
    public static final String GAMES = "games";
    public static final String HELP = "help";
    public static final String MAIL = "mail";
    public static final String ME = "me";
    public static final String PEOPLE = "people";
    public static final String SHOP = "shop";
    public static final String STUFF = "stuff";
    public static final String SWIFTLY = "swiftly";
    public static final String WHIRLEDS = "whirleds";
    public static final String WORLD = "world";

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
        // let the frame know about our content table
        Frame.initContent(_content);
    }

    /**
     * Called when the user navigates to this page for the first time, and when they follow {@link
     * Application#createLink} links within tihs page.
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
        return (_content != null && _content.isCellPresent(1, 0)) ? _content.getWidget(1, 0) : null;
    }

    /**
     * Clears out any existing content, creates a new Flash object from the definition, and sets it
     * as the new main page content. Returns the newly-created content as a widget.
     */
    protected HTML setFlashContent (String definition)
    {
        // Please note: the following is a work-around for an IE7 bug. If we create a Flash object
        // node *before* attaching it to the DOM tree, IE will silently fail to register the Flash
        // object's callback functions for access from JavaScript. To make this work, create an
        // empty node first, add it to the DOM tree, and then initialize it with the Flash object
        // definition.  Also see: WidgetUtil.embedFlashObject()
        HTML control = new HTML();
        setContent(control);
        control.setHTML(definition);
        return control;
    }

    /**
     * Clears out any existing content and sets the specified widget as the main page content.
     */
    protected void setContent (Widget content)
    {
        setContent(content, true);
    }

    /**
     * Clears out any existing content and sets the specified widget as the main page content.
     */
    protected void setContent (Widget content, boolean withHeader)
    {
        Frame.setHeaderVisible(withHeader);
        if (withHeader) {
            Frame.showContent(getTabPageId(), _content);
            _content.setContent(content);
        } else {
            Frame.showContent(getTabPageId(), content);
        }
    }

    protected void setPageTabs (Widget tabs)
    {
        _content.setPageTabs(tabs);
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

    protected static class Content extends SmartTable
    {
        public Content (String pageId) {
            super("pageContent", 0, 0);
            setWidth("100%");
            setHeight("100%");

            // a separate table for the header, so that we can set individual cell widths correctly
            _tabs = new SmartTable("pageHeader", 0, 0);
            _tabs.getFlexCellFormatter().setStyleName(0, 0, "TitleCell");
            _tabs.getFlexCellFormatter().setWidth(0, 0, "118px");
            _tabs.getFlexCellFormatter().setHorizontalAlignment(0, 0, HasAlignment.ALIGN_CENTER);
            _tabs.getFlexCellFormatter().setStyleName(0, 1, "TabsCell");

            _closeBox = MsoyUI.createActionLabel("", "CloseBox", new ClickListener() {
                public void onClick (Widget sender) {
                    Frame.closeContent();
                }
            });
            _tabs.setWidget(0, 2, _closeBox);
            _tabs.getFlexCellFormatter().setStyleName(0, 2, "CloseCell");
            setCloseVisible(false);

            setWidget(0, 0, _tabs, 1, "Bar");
            getFlexCellFormatter().addStyleName(
                0, 0, pageId.toUpperCase().substring(0, 1) + pageId.substring(1) + "Bar");

            getFlexCellFormatter().setHeight(1, 0, "100%");
            getFlexCellFormatter().setVerticalAlignment(1, 0, HasAlignment.ALIGN_TOP);
        }

        public void setPageTabs (Widget tabs) {
//             _tabs.setWidget(0, 1, tabs);
        }

        public void setCloseVisible (boolean visible) {
            _closeBox.setVisible(visible);
        }

        public void setContent (Widget content) {
            setWidget(1, 0, content);

            // if there isn't anything in the tabs/subtitle area, we need something there to cause
            // IE to properly use up the space
            if (_tabs.getWidget(0, 1) == null && _tabs.getText(0, 1).length() == 0) {
                _tabs.setHTML(0, 1, "&nbsp;");
            }
        }

        protected SmartTable _tabs;
        protected Label _closeBox;
    }

    protected Content _content = new Content(getTabPageId());
}
