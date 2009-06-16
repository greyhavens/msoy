//
// $Id$

package client.facebook;

import client.shell.CShell;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Wraps web content containing FBML code in a <code>fb:serverfbml</code> panel, for use within a
 * Facebook iframed application. Once all FBML code is nested or after something changes, call
 * {@link #reparse} to tell Facebook to reparse the DOM tree and load up the new fbml code.
 */
public class ServerFBMLPanel extends Composite
{
    /**
     * Creates a new server FBML panel.
     */
    public ServerFBMLPanel ()
    {
        _panel = new FBMLPanel("serverfbml");
        _panel.setId(HTMLPanel.createUniqueId());
        initWidget(_panel);
    }

    /**
     * Adds a widget to the panel.
     */
    public void add (Widget w)
    {
        _panel.add(w);
    }

    /**
     * Removes a widget from the panel.
     */
    public void remove (Widget w)
    {
        _panel.remove(w);
    }

    /**
     * Tells Facebook to reparse our DOM tree and instantiate FML elements.
     */
    public void reparse ()
    {
        CShell.log("Reparsing XFBML");
        // TODO: pass getElement().getId()
        nreparse();
    }

    protected static native void nreparse () /*-{
        try {
            $wnd.FB_ParseXFBML();
        } catch (e) {
            if ($wnd.console) {
                $wnd.console.log("Failed to reparse XFBML [error=" + e + "]");
            }
        }
    }-*/;

    protected FBMLPanel _panel;
}
