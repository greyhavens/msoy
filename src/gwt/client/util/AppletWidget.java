//
// $Id$

package client.util;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Widget;

import client.util.impl.AppletWidgetImpl;

/**
 * Contains the various jiggery pokery needed to display a Java Applet.
 */
public class AppletWidget extends Widget
{
    public AppletWidget (String ident)
    {
        // create our browser-specific implementation
        _impl = (AppletWidgetImpl)GWT.create(AppletWidgetImpl.class);
        setElement(_elem = _impl.createElement(ident));
        setStyleName("gwt-AppletWidget");

        // set a default size
        setPixelSize(400, 400);
    }

    /**
     * Configures the applet archive and class.
     */
    public void setApplet (String archive, String clazz)
    {
        _impl.setApplet(_elem, archive, clazz);
    }

    /**
     * Adds a <param> tag to the applet.
     */
    public void addParam (String name, String value)
    {
        DOM.appendChild(_elem, WidgetUtil.createParam(name, value));
    }

    // @Override (UIObject)
    public void setHeight (String height)
    {
        DOM.setAttribute(_elem, "height", WidgetUtil.ensurePixels(height));
    }

    // @Override (UIObject)
    public void setWidth (String width)
    {
        DOM.setAttribute(_elem, "width", WidgetUtil.ensurePixels(width));
    }

    protected AppletWidgetImpl _impl;
    protected Element _elem;
}
