//
// $Id$

package client.util;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.HTML;

/**
 * Contains some widget-related utility functions.
 */
public class WidgetUtil
{
    /**
     * Creates the necessary bullshit HTML to display a Flash movie in a way
     * that doesn't bunch up the panties of one or more of the goddamned web
     * browsers that we're trying to support.
     *
     * TODO: redo this with browser specific stuff so that we don't have tag ID
     * conflicts.
     */
    public static HTML createFlashMovie (
        String ident, String movie, int width, int height)
    {
        return new HTML(
            "<object id=\"" + ident + "\" " + // IE crap
            "width=\"" + width + "\" height=\"" + height + "\" " +
            "classid=\"clsid:d27cdb6e-ae6d-11cf-96b8-444553540000\" " +
            "codebase=\"http://active.macromedia.com/flash7/cabs/" +
            "swflash.cab#version=9,0,0,0\">" +
            "<param name=\"movie\" value=\"" + movie + "\">" +
            "<comment>" + // Mozilla crap
            "<embed type=\"application/x-shockwave-flash\" " +
            "pluginspage=\"http://www.macromedia.com/go/getflashplayer\" " +
            "width=\"" + width + "\" height=\"" + height + "\" " +
            "src=\"" + movie + "\"/>" +
            "</comment>" +
            "</object>");
    }

    /**
     * Creates the necessary bullshit HTML to display a Java Applet in a way
     * that doesn't bunch up the panties of one or more of the goddamned web
     * browsers that we're trying to support.
     *
     * TODO: redo this with browser specific stuff so that we don't have tag ID
     * conflicts.
     */
    public static HTML createApplet (
        String ident, String archive, String clazz,
        int width, int height, String[] params)
    {
        String ieobj = "<object " +
            "classid=\"clsid:8AD9C840-044E-11D1-B3E9-00805F499D93\" " +
            "width=\"" + width + "\" height=\"" + height + "\" " +
            "codebase=\"http://java.sun.com/update/1.5.0/" +
            "jinstall-1_5-windows-i586.cab#Version=5,0,0,5\">";
        String mozobj = "<object classid=\"java:" + clazz + ".class\" " +
            "type=\"application/x-java-applet\" archive=\"" + archive + "\" " +
            "width=\"" + width + "\" height=\"" + height + "\">";
        String ptags = "";
        for (int ii = 0; ii < params.length; ii += 2) {
            ptags = ptags + "<param name=\"" + params[ii] + "\" " +
                "value=\"" + params[ii+1] + "\"/>";
        }
        return new HTML(
            mozobj + ptags + ieobj +
            "<param name=\"code\" value=\"" + clazz + "\"/>" +
            "<param name=\"archive\" value=\"" + archive + "\"/>" +
            ptags + "</object></object>");
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
}
