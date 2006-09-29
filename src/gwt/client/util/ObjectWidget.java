//
// $Id$

package client.util;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Widget;

/**
 * A base class containing shared functionality for {@link FlashWidget} and
 * {@link AppletWidget}.
 */
public class ObjectWidget extends Widget
{
    protected Element createParam (String name, String value)
    {
        Element pelem = DOM.createElement("param");
        DOM.setAttribute(pelem, "name", name);
        DOM.setAttribute(pelem, "value", value);
        return pelem;
    }

    /**
     * Chop off any non-numeric suffix.
     */
    protected String ensurePixels (String value)
    {
        int index = 0;
        for (int nn = value.length(); index < nn; index++) {
            char c = value.charAt(index);
            if (c < '0' || c > '9') {
                break;
            }
        }
        return value.substring(0, index);
    }
}
