//
// $Id$

package client.util;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Widget;

/**
 * Contains the various jiggery pokery needed to display a Flash movie.
 */
public class FlashWidget extends Widget
{
    public FlashWidget (String ident)
    {
        // our outer element is <object>
        setElement(DOM.createElement("object"));
        setStyleName("gwt-FlashWidget");

        // configure our <object> element
        DOM.setAttribute(getElement(), "classid", "peeny");
        DOM.setAttribute(getElement(), "floogle",
                         "clsid:d27cdb6e-ae6d-11cf-96b8-444553540000");
        DOM.setAttribute(getElement(), "codebase",
                         "http://active.macromedia.com/flash7/cabs/" +
                         "swflash.cab#version=9,0,0,0");
        DOM.setAttribute(getElement(), "id", ident);

        // create some child <param> tags
        DOM.appendChild(getElement(), _bgcolor = createParam("bgcolor", ""));
        DOM.appendChild(getElement(), _quality = createParam("quality", ""));
        DOM.appendChild(getElement(), _movie = createParam("movie", ""));

        // create and configure our <embed> element
        DOM.appendChild(getElement(), _embed = DOM.createElement("embed"));
        DOM.setAttribute(_embed, "type", "application/x-shockwave-flash");
        DOM.setAttribute(_embed, "pluginspage",
                         "http://www.macromedia.com/go/getflashplayer");
        DOM.setAttribute(_embed, "name", ident);

        // set up some defaults, filling in both the embed and param tags
        setBackgroundColor("#FFFFFF");
        setQuality("high");
        setSize(400, 400);
    }

    public void setMovie (String path)
    {
        DOM.setAttribute(_movie, "value", path);
        DOM.setAttribute(_embed, "src", path);
    }

    public void setQuality (String quality)
    {
        DOM.setAttribute(_quality, "value", quality);
        DOM.setAttribute(_embed, "quality", quality);
    }

    public void setBackgroundColor (String bgcolor)
    {
        DOM.setAttribute(_bgcolor, "value", bgcolor);
        DOM.setAttribute(_embed, "bgcolor", bgcolor);
    }

    public void setSize (int width, int height)
    {
        String strWidth = String.valueOf(width);
        String strHeight = String.valueOf(height);
        DOM.setAttribute(getElement(), "width", strWidth);
        DOM.setAttribute(_embed, "width", strWidth);
        DOM.setAttribute(getElement(), "height", strHeight);
        DOM.setAttribute(_embed, "height", strHeight);
    }

    protected Element createParam (String name, String value)
    {
        Element pelem = DOM.createElement("param");
        DOM.setAttribute(pelem, "name", name);
        DOM.setAttribute(pelem, "value", value);
        return pelem;
    }

    protected Element _embed;
    protected Element _quality;
    protected Element _bgcolor;
    protected Element _movie;
}
