//
// $Id$

package client.util;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Image;

public class SizeableImage extends Image
{
    public SizeableImage ()
    {
        super();
    }

    public SizeableImage (String url)
    {
        super(url);
    }

    // @Override (UIObject
    public void setHeight (String height)
    {
        DOM.setAttribute(getElement(), "height", height);
    }

    // @Override (UIObject
    public void setWidth (String width)
    {
        DOM.setAttribute(getElement(), "width", width);
    }
}
