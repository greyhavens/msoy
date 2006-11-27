//
// $Id$

package client.util;

/**
 * Wraps the necessary JavaScript fiddling to read and write cookies on the
 * client.
 */
public class CookieUtil
{
    /**
     * Sets the specified cookie to the supplied value.
     */
    public static native void set (String name, String value) /*-{
        $doc.cookie = "\"" + name + "=" + escape(value) + "\"";
    }-*/;

    /**
     * Sets the specified cookie to the supplied value.
     */
    public static native void set (String path, String name, String value) /*-{
        $doc.cookie = "\"" + name + "=" + escape(value) + "; path=" + path + "\"";
    }-*/;

    /**
     * Sets the specified cookie to the supplied value.
     */
    public static native void set (String domain, String path, String name, String value) /*-{
        $doc.cookie = "\"" + name + "=" + escape(value) + "; domain=" + domain +
            "; path=" + path + "\"";
    }-*/;

    /**
     * Sets the specified cookie to the supplied value.
     */
    public static native void set (String domain, String path, String expires,
                                   String name, String value) /*-{
        $doc.cookie = "\"" + name + "=" + escape(value) + "; domain=" + domain + "; path=" + path +
            "; expires=" + expires + "\"";
    }-*/;

    /**
     * Looks up and returns the value for the specified cookie.
     */
    public static native String get (String name) /*-{
        var dc = $doc.cookie;
        var prefix = name + "=";
        var begin = dc.indexOf("; " + prefix);
        if (begin == -1) {
            begin = dc.indexOf(prefix);
            if (begin != 0) {
                return null;
            }
        } else {
            begin += 2;
        }
        var end = $doc.cookie.indexOf(";", begin);
        if (end == -1) {
            end = dc.length;
        }
        return unescape(dc.substring(begin + prefix.length, end));
     }-*/;
}
