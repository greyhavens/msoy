package client.ui.impl;

import com.google.gwt.dom.client.Element;

/**
 * Correctly accesses the href attribute of an anchor tag in IE8.
 */
public class HrefImplIE8 extends HrefImpl
{
    @Override // from HrefImpl
    public native String getLiteral (Element elem) /*-{
        return elem.getAttribute("href", 2) || "";
    }-*/;
}
