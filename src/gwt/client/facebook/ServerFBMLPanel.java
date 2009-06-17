//
// $Id$

package client.facebook;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Wraps web content containing FBML code in a <code>fb:serverfbml</code> panel, for use within a
 * Facebook iframed application. Normally, this is only necessary for non-XFBML tags, i.e. tags
 * that take input from the user like <code>fb:request-form</code>. Once all FBML code is nested
 * call {@link #render}. This will do the jiggery-pokery necessary to wrap the interior in a script
 * tag and tell Facebook to reparse the DOM tree and load up the new fbml code.
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
     * Once all descendants are in place, does the inner html hackery to get everything into an
     * fbml script tag.
     */
    public void render ()
    {
        String fbmlCode = getElement().getInnerHTML();
        _panel.clear();
        getElement().setInnerHTML("<script type=\"text/fbml\"><fb:fbml>" +
            fbmlCode + "</fb:fbml></script>");
        FBMLPanel.reparse(_panel);
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

    protected FBMLPanel _panel;
}
