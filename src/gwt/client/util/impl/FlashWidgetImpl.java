//
// $Id$

package client.util.impl;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

/**
 * Contains the various jiggery pokery needed to display a Flash movie on a
 * normal browser.
 */
public class FlashWidgetImpl
{
    public Element createElement (String ident)
    {
        // create and configure our <object> element
        Element elem = DOM.createElement("object");
        DOM.setAttribute(elem, "classid",
                         "clsid:d27cdb6e-ae6d-11cf-96b8-444553540000");
        DOM.setAttribute(elem, "codebase",
                         "http://active.macromedia.com/flash7/cabs/" +
                         "swflash.cab#version=9,0,0,0");
        DOM.setAttribute(elem, "id", ident);

        // create some child <param> tags
        DOM.appendChild(elem, _bgcolor = createParam("bgcolor", ""));
        DOM.appendChild(elem, _quality = createParam("quality", ""));
        DOM.appendChild(elem, _movie = createParam("movie", ""));

        return elem;
    }

    public void setBackgroundColor (Element elem, String bgcolor)
    {
        DOM.setAttribute(_bgcolor, "value", bgcolor);
    }

    public void setQuality (Element elem, String quality)
    {
        DOM.setAttribute(_quality, "value", quality);
    }

    public void setMovie (Element elem, String path)
    {
        DOM.setAttribute(_movie, "value", path);
    }

    public void setHeight (Element elem, String height)
    {
        DOM.setAttribute(elem, "height", height);
    }

    public void setWidth (Element elem, String width)
    {
        DOM.setAttribute(elem, "width", width);
    }

    protected Element createParam (String name, String value)
    {
        Element pelem = DOM.createElement("param");
        DOM.setAttribute(pelem, "name", name);
        DOM.setAttribute(pelem, "value", value);
        return pelem;
    }

    protected Element _bgcolor;
    protected Element _quality;
    protected Element _movie;
}
