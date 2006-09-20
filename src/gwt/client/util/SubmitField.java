//
// $Id$

package client.util;

import com.google.gwt.user.client.DOM;

import com.google.gwt.user.client.ui.Widget;

/**
 * A form submit button.
 */
public class SubmitField extends Widget
{
    public SubmitField (String name, String value)
    {
        setElement(DOM.createElement("input"));
        DOM.setAttribute(getElement(), "type", "submit");
        DOM.setAttribute(getElement(), "value", value);
        DOM.setAttribute(getElement(), "name", name);
    }
}
