package client.ui.impl;

import com.google.gwt.dom.client.Element;

/**
 * Deferred binding implementation for dealing with the quirks of different browsers in accessing
 * the href attribute of an anchor tag.
 * @see http://www.glennjones.net/Post/809/getAttributehrefbug.htm
 */
public class HrefImpl
{
    /**
     * Gets the literal value for an href attribute (not the absolute URL).
     */
    public String getLiteral (Element element)
    {
        return element.getAttribute("href");
    }

    /**
     * Sets the literal value for an href attribute. This does not appear to be quirky bue is
     * included here for symmetry.
     */
    public void setLiteral (Element element, String value)
    {
        element.setAttribute("href", value);
    }
}
