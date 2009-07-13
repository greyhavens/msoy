//
// $Id$

package client.facebook;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Wraps web content containing FBML code in a <code>fb:serverfbml</code> panel, for use within a
 * Facebook iframed application. Normally, this is only necessary for non-XFBML tags, i.e. tags
 * that take input from the user like <code>fb:request-form</code>. Once the widget is loaded
 * ({@link Widget#onLoad}), the render method is called. This will do the jiggery-pokery necessary
 * to wrap the interior in a script tag and tell Facebook to reparse the DOM tree and load up the
 * new fbml code.
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
     * Once all descendants are in place, does the DOM hackery to get everything into an fbml
     * script tag. This could be done in the constructor, but IE reportedly does not support some
     * element operations prior to the element being added to the document, so better safe than
     * sorry.
     */
    @Override protected void onLoad ()
    {
        super.onLoad();

        // render only once, just to be sure
        if (_rendered) {
            return;
        }
        _rendered = true;
        render();
    }

    /**
     * Does the various bits of hackery to wrap up our children into script/fbml tags, swap that in
     * as our content, then reparse our FBML stuffs.
     */
    protected void render ()
    {
        // first grab all of our child widgets as html (e.g. "<fb:request-form>...")
        String fbmlCode = "<fb:fbml>" + getElement().getInnerHTML() + "</fb:fbml>";

        // get rid of our children
        _panel.clear();

        // add a fbml script element
        Element script = DOM.createElement("script");
        script.setAttribute("type", "text/fbml");
        getElement().appendChild(script);

        // now for the tricky bit:
        //   1. GWT and/or DOM does not support adding child nodes to a script tag
        //   2. IE does not support setting innerHTML on script tags (it just silently blanks it)
        //   3. IE crashes when setting innerText on script tags
        // so... write to the "text" property of the script tag. voila
        // http://www.codingforums.com/archive/index.php/t-39551.html
        script.setPropertyString("text", fbmlCode);

        // finally tell Facebook to do its dirty business
        _panel.reparse();
    }

    protected FBMLPanel _panel;
    protected boolean _rendered;
}
