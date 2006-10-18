//
// $Id$

package client.util;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.HTML;

/**
 * Contains some widget-related utility functions.
 */
public class WidgetUtil
{
    /**
     * Creates the HTML to display a Flash movie for the browser on which we're
     * running.
     *
     * @param flashVars a pre-URLEncoded string containing flash variables,
     *        or null.
     *        http://www.adobe.com/cfusion/knowledgebase/index.cfm?id=tn_16417
     */
    public static HTML createFlashMovie (
        String ident, String movie, int width, int height, String flashVars)
    {
        return createFlashMovie(ident, movie, ""+width, ""+height, flashVars);
    }

    /**
     * Creates the HTML to display a Flash movie for the browser on which we're
     * running.
     *
     * @param flashVars a pre-URLEncoded string containing flash variables,
     *        or null.
     *        http://www.adobe.com/cfusion/knowledgebase/index.cfm?id=tn_16417
     */
    public static HTML createFlashMovie (
        String ident, String movie, String width, String height,
        String flashVars)
    {
        return _impl.createFlashMovie(ident, movie, width, height, flashVars);
    }

    /**
     * Creates the HTML to display a Java applet for the browser on which we're
     * running.
     */
    public static HTML createApplet (
        String ident, String archive, String clazz,
        int width, int height, String[] params)
    {
        return createApplet(ident, archive, clazz, ""+width, ""+height, params);
    }

    /**
     * Creates the HTML to display a Java applet for the browser on which we're
     * running.
     */
    public static HTML createApplet (
        String ident, String archive, String clazz,
        String width, String height, String[] params)
    {
        String ptags = "";
        for (int ii = 0; ii < params.length; ii += 2) {
            ptags = ptags + "<param name=\"" + params[ii] + "\" " +
                "value=\"" + params[ii+1] + "\"/>";
        }
        return _impl.createApplet(ident, archive, clazz, width, height, ptags);
    }

    /**
     * Creates a <param> tag with the supplied name and value.
     */
    public static Element createParam (String name, String value)
    {
        Element pelem = DOM.createElement("param");
        DOM.setAttribute(pelem, "name", name);
        DOM.setAttribute(pelem, "value", value);
        return pelem;
    }

    /**
     * Chops off any non-numeric suffix.
     */
    public static String ensurePixels (String value)
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

    protected static WidgetUtilImpl _impl;

    static {
        _impl = (WidgetUtilImpl)GWT.create(WidgetUtilImpl.class);
    }
}
