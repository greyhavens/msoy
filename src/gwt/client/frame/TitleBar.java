//
// $Id$

package client.frame;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.Tabs;

/**
 * Displays a page title and subnavigation at the top of the page content area.
 */
public abstract class TitleBar
{
    /**
     * Creates a new title bar for web content.
     * @param layout determines some features of the title bar such as tabs usage and whether the
     * back button is shown
     * @param tab the tab to select if applicable
     * @param onClose the callback to invoke when the "close" button is clicked
     */
    public static TitleBar create (Layout layout, Tabs tab, ClickHandler onClose)
    {
        if (layout instanceof FacebookLayout) {
            return new FacebookTitleBar(false);
        }

        StandardTitleBar titleBar = new StandardTitleBar(tab, onClose, layout.usesFramedTitleBar());
        return titleBar;
    }

    /**
     * Creates a new title bar for flash content.
     * @param layout determines some features of the title bar such as tabs usage and whether the
     * back button is shown
     * @param tab the tab to select if applicable
     * @param onClose the callback to invoke when the "close" button is clicked
     * @return null if the layout does not show the title bar on flash
     */
    public static TitleBar createClient (Layout layout, boolean inGame, ClickHandler onClose)
    {
        if (!(layout instanceof FacebookLayout) || !inGame) {
            return null;
        }
        return new FacebookTitleBar(true);
    }

    /**
     * Sets the visibility of the client close box.
     */
    public abstract void setCloseVisible (boolean visible);

    /**
     * Sets the text shown to the given title.
     */
    public abstract void setTitle (String title);

    /**
     * Resets the navigation links. Used when the user logs in or out or to remove previously added
     * context links.
     */
    public abstract void resetNav ();

    /**
     * Exposes the underlying widget that should be attached to the main frame in the title bar
     * position.
     */
    public abstract Widget exposeWidget ();

    /**
     * Adds a new link to the subnavigation area required by the page context (e.g. the shop page
     * adds an avatars link when the user is viewing the avatars subpage).
     * @param label the text of the link
     * @param page the page to link to
     * @param args the token to link to within the page 
     * @param position the index within the existing links
     */
    public abstract void addContextLink (String label, Pages page, Args args, int position);
}
