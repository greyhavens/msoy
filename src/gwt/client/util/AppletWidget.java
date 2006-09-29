//
// $Id$

package client.util;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

/**
 * Contains the various jiggery pokery needed to display a Java Applet.
 */
public class AppletWidget extends ObjectWidget
{
    public AppletWidget (String ident)
    {
        // our outer element is an <object> (for Mozilla)
        setElement(DOM.createElement("object"));
        setStyleName("gwt-AppletWidget");

        // configure our outer <object> element
        DOM.setAttribute(getElement(), "type", "application/x-java-applet");
        DOM.setAttribute(getElement(), "id", ident);

        // create and configure our inner <object> element (for MSIE)
        DOM.appendChild(getElement(), _inner = DOM.createElement("object"));
        DOM.setAttribute(_inner, "classid",
                         "clsid:8AD9C840-044E-11D1-B3E9-00805F499D93");
        DOM.setAttribute(_inner, "codebase", "http://java.sun.com/update/" +
                         "1.5.0/jinstall-1_5-windows-i586.cab#Version=5,0,0,5");
        DOM.setAttribute(_inner, "name", ident);
        DOM.appendChild(_inner, _clazz = createParam("code", ""));
        DOM.appendChild(_inner, _archive = createParam("archive", ""));

        // set up some defaults, filling in both the embed and param tags
        setPixelSize(400, 400);
    }

    public void setApplet (String archive, String clazz)
    {
        DOM.setAttribute(getElement(), "archive", archive);
        DOM.setAttribute(getElement(), "classid", "java:" + clazz + ".class");
        DOM.setAttribute(_clazz, "value", clazz);
        DOM.setAttribute(_archive, "value", archive);
    }

    /**
     * Adds a <param> tag to the applet.
     */
    public void addParam (String name, String value)
    {
        DOM.appendChild(getElement(), createParam(name, value));
        DOM.appendChild(_inner, createParam(name, value));
    }

    // @Override (UIObject)
    public void setHeight (String height)
    {
        height = ensurePixels(height);
        DOM.setAttribute(getElement(), "height", height);
        DOM.setAttribute(_inner, "height", height);
    }

    // @Override (UIObject)
    public void setWidth (String width)
    {
        width = ensurePixels(width);
        DOM.setAttribute(getElement(), "width", width);
        DOM.setAttribute(_inner, "width", width);
    }

    protected Element _inner, _clazz, _archive;
}
