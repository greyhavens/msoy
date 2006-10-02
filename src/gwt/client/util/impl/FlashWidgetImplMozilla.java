//
// $Id$

package client.util.impl;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

/**
 * Contains the various jiggery pokery needed to display a Flash movie in
 * Mozilla.
 */
public class FlashWidgetImplMozilla extends FlashWidgetImpl
{
    public Element createElement (String ident)
    {
        // create and configure our <embed> element
        Element elem = DOM.createElement("embed");
        DOM.setAttribute(elem, "type", "application/x-shockwave-flash");
        DOM.setAttribute(elem, "pluginspage",
                         "http://www.macromedia.com/go/getflashplayer");
        DOM.setAttribute(elem, "name", ident);
        return elem;
    }

    public void setBackgroundColor (Element elem, String bgcolor)
    {
        DOM.setAttribute(elem, "bgcolor", bgcolor);
    }

    public void setQuality (Element elem, String quality)
    {
        DOM.setAttribute(elem, "quality", quality);
    }

    public void setMovie (Element elem, String path)
    {
        DOM.setAttribute(elem, "src", path);
    }
}
