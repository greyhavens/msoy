//
// $Id$

package client.util;

import com.google.gwt.user.client.ui.HTML;

/**
 * Provides browser-specific widget-related utility functions.
 */
public class WidgetUtilImpl
{
    /**
     * Creates the HTML needed to display a Flash movie.
     */
    public HTML createFlashMovie (
        String ident, String movie, String width, String height,
        String flashVars)
    {
        String params = "<param name=\"movie\" value=\"" + movie + "\">";
        if (flashVars != null) {
            params += "<param name=\"FlashVars\" value=\"" + flashVars + "\">";
        }

        return new HTML(
            "<object id=\"" + ident + "\" " + // IE crap
            "width=\"" + width + "\" height=\"" + height + "\" " +
            "classid=\"clsid:d27cdb6e-ae6d-11cf-96b8-444553540000\" " +
            "codebase=\"http://active.macromedia.com/flash7/cabs/" +
            "swflash.cab#version=9,0,0,0\">" + params + "</object>");
    }

    /**
     * Creates the HTML needed to display a Java applet.
     */
    public HTML createApplet (
        String ident, String archive, String clazz,
        String width, String height, String ptags)
    {
        return new HTML(
            "<object classid=\"clsid:8AD9C840-044E-11D1-B3E9-00805F499D93\" " +
            "width=\"" + width + "\" height=\"" + height + "\" " +
            "codebase=\"http://java.sun.com/update/1.5.0/" +
            "jinstall-1_5-windows-i586.cab#Version=5,0,0,5\">" +
            "<param name=\"code\" value=\"" + clazz + "\"/>" +
            "<param name=\"archive\" value=\"" + archive + "\"/>" +
            ptags + "</object>");
    }
}
