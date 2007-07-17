//
// $Id$

package client.shell;

import java.util.ArrayList;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

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
     * Splits arguments composed by a call to {@link #composeArgs}. Any invalid integers will be
     * converted to 0.
     */
    public static int[] splitArgs (String args)
    {
        ArrayList alist = new ArrayList();
        do {
            int didx = args.indexOf(ARG_SEP);
            if (didx == -1) {
                alist.add(args);
                args = null;
            } else {
                alist.add(args.substring(0, didx));
                args = args.substring(didx+1);
            }
        } while (args != null && args.length() > 0);

        int[] values = new int[alist.size()];
        for (int ii = 0; ii < values.length; ii++) {
            try {
                values[ii] = Integer.parseInt((String)alist.get(ii));
            } catch (Exception e) {
                values[ii] = 0;
            }
        }
        return values;
    }

    /**
     * Composes multiple integers into a single string argument that can be split up again with a
     * call to {@link #splitArgs}.
     */
    public static String composeArgs (int[] args)
    {
        StringBuffer builder = new StringBuffer();
        for (int ii = 0; ii < args.length; ii++) {
            if (ii > 0) {
                builder.append(ARG_SEP);
            }
            builder.append(args[ii]);
        }
        return builder.toString();
    }

    /** 
     * Notes the history token for the current page so that it can be restored in the event that we
     * open a normal page and then later close it.
     *
     * @return true if we're displaying a different page (or the same page with different
     * arguments) than the last time we entered "showing client" mode.
     */
    public static boolean setShowingClient (boolean clientIsFlash, boolean clientIsJava)
    {
        // determine whether or not we're showing a new page
        boolean newPage = !History.getToken().equals(_closeToken);

        // note the current history token so that we can restore it if needed
        _closeToken = History.getToken();

        // note whether we need to hack our popups
        displayingFlash = clientIsFlash;
        displayingJava = clientIsJava;

        // clear out our content and the expand/close controls
        RootPanel.get("content").clear();
        RootPanel.get("content").setWidth("0px");

        // have the client take up all the space
        RootPanel.get("client").setWidth("100%");

        return newPage;
    }

    /**
     * Let the currently showing client reign supreme.
     */
    public static void closePage ()
    {
        if (_closeToken != null) {
            History.newItem(_closeToken);
        }
    }

    /**
     * Returns true if we need to do our popup hackery, false if not.
     */
    public static boolean needPopupHack ()
    {
        // if we're displaying a Java applet, we always need the popup hack, but for Flash we only
        // need it on Linux
        return displayingJava || (displayingFlash && isLinux());
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
     * Used to remove the close button, either because we've been removed from view, or beacuse the
     * flash client has been removed.
     */
    public void clearCloseButton ()
    {
        if (_content != null) {
            _closeToken = null;
            _content.setText(0, 2, "");
            _content.setText(0, 3, "");
        }
    }

    protected void setCloseButton ()
    {
        if (_closeToken != null) {
            _content.setWidget(0, 2, MsoyUI.createActionLabel("", "CloseBox", new ClickListener() {
                public void onClick (Widget sender) {
                    closePage();
                }
            }));
            _content.setWidget(0, 3, MsoyUI.createLabel("", "Separator"));
        }
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
        setContentInternal(content, false, false);
    }

    /**
     * Clears out any existing content, creates a new Flash object from the definition, and 
     * sets it as the new main page content. Returns the newly-created content as a widget.
     */
    protected HTML setFlashContent (String definition)
    {
        // Please note: the following is a work-around for an IE7 bug. If we create a Flash object
        // node *before* attaching it to the DOM tree, IE will silently fail to register
        // the Flash object's callback functions for access from JavaScript. To make this work,
        // create an empty node first, add it to the DOM tree, and then initialize it with
        // the Flash object definition.
        // Also see: WidgetUtil.embedFlashObject()
        HTML control = new HTML();
        setContentInternal(control, true, false);
        control.setHTML(definition);
        return control;
    }
    
    /**
     * Clears out any existing content and sets the specified Java applet as the main page content.
     */
    protected void setJavaContent (Widget content)
    {
        setContentInternal(content, false, true);
    }

    /**
     * Clears out any existing content and sets the specified widget as the main page content.
     */
    protected void setContentInternal (
        Widget content, boolean contentIsFlash, boolean contentIsJava)
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
        RootPanel.get("content").add(_content);
        _content.setWidget(1, 0, content);
        // if there isn't anything in the tabs area, the name ends up centering over the whole
        // display.
        if (_content.getWidget(0, 1) == null) {
            _content.setHTML(0, 1, "&nbsp;");
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
        _content.setHeight("100%");
        _content.getFlexCellFormatter().setStyleName(0, 0, "pageHeaderTitle");
        _content.getFlexCellFormatter().setStyleName(0, 1, "pageHeaderContent");
        _content.getFlexCellFormatter().setStyleName(0, 2, "pageHeaderClose");
        _content.getFlexCellFormatter().setStyleName(0, 3, "pageHeaderSep");
        _content.getFlexCellFormatter().setColSpan(1, 0, 4);
        _content.getFlexCellFormatter().setHeight(1, 0, "100%");

        setCloseButton();
    }

    protected void setPageTitle (String title)
    {
        if (_content == null) {
            createContentContainer();
        }
        _content.setWidget(0, 0, new Label(title));
        Window.setTitle(CShell.cmsgs.windowTitle(title));
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
        onHistoryChanged(getPageArgs());
    }

    /**
     * Called when the player logs off while viewing this page. The default implementation
     * redisplays the current page with the current args (by calling {@link #onHistoryChanged}).
     */
    protected void didLogoff ()
    {
        onHistoryChanged(getPageArgs());
    }

    protected static native boolean isLinux () /*-{
        return (navigator.userAgent.toLowerCase().indexOf("linux") != -1);
    }-*/;

    protected FlexTable _content;

    protected static String _closeToken;

    protected static final String ARG_SEP = "_";
}
