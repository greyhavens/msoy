//
// $Id$

package client.util;

import com.google.gwt.user.client.ui.HTML;

/**
 * Provides Mozilla-specific widget-related utility functions.
 */
public class WidgetUtilImplMozilla extends WidgetUtilImpl
{
    /**
     * Creates the HTML needed to display a Flash movie.
     */
    public HTML createFlashContainer (String ident, String movie, String width, String height,
                                      String flashVars)
    {
        String params = "";
        if (flashVars != null) {
            params += "FlashVars=\"" + flashVars + "\" "; // trailing space
        }

        String tag = "<embed type=\"application/x-shockwave-flash\" " +
            "pluginspage=\"http://www.macromedia.com/go/getflashplayer\"";
        if (width.length() > 0) {
            tag += " width=\"" + width + "\"";
        }
        if (height.length() > 0) {
            tag += " height=\"" + height + "\"";
        }
        tag += " src=\"" + movie + "\" allowScriptAccess=\"sameDomain\"" +
            " allowFullScreen=\"true\" " + params + "/>";
        return new HTML(tag);
    }

    /**
     * Creates the HTML needed to display a Java applet.
     */
    public HTML createApplet (String ident, String archive, String clazz,
                              String width, String height, String ptags)
    {
        return new HTML(
            "<object classid=\"java:" + clazz + ".class\" " +
            "type=\"application/x-java-applet\" archive=\"" + archive + "\" " +
            "width=\"" + width + "\" height=\"" + height + "\">" +
            ptags + "</object>");
    }
}
