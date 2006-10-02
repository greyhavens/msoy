//
// $Id$

package client.util;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

import client.util.impl.FlashWidgetImpl;

/**
 * Contains the various jiggery pokery needed to display a Flash movie.
 */
public class FlashWidget extends ObjectWidget
{
    public FlashWidget (String ident)
    {
        // create our browser-specific implementation
        _impl = (FlashWidgetImpl)GWT.create(FlashWidgetImpl.class);
        setElement(_elem = _impl.createElement(ident));
        setStyleName("gwt-FlashWidget");

        // set up some defaults, filling in both the embed and param tags
        setBackgroundColor("#FFFFFF");
        setQuality("high");
        setPixelSize(400, 400);
    }

    public void setBackgroundColor (String bgcolor)
    {
        _impl.setBackgroundColor(_elem, bgcolor);
    }

    public void setQuality (String quality)
    {
        _impl.setQuality(_elem, quality);
    }

    public void setMovie (String path)
    {
        _impl.setMovie(_elem, path);
    }

    // @Override (UIObject)
    public void setHeight (String height)
    {
        _impl.setHeight(_elem, ensurePixels(height));
    }

    // @Override (UIObject)
    public void setWidth (String width)
    {
        _impl.setWidth(_elem, ensurePixels(width));
    }

    protected FlashWidgetImpl _impl;
    protected Element _elem;
}
