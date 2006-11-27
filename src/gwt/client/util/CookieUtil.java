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
    public static void set (String name, String value)
    {
        set("", "", "", name, value);
    }

    /**
     * Sets the specified cookie to the supplied value.
     */
    public static void set (String path, String name, String value)
    {
        set("", path, "", name, value);
    }

    /**
     * Sets the specified cookie to the supplied value.
     */
    public static void set (String domain, String path, String name, String value)
    {
        set(domain, path, "", name, value);
    }

    /**
     * Sets the specified cookie to the supplied value.
     */
    public static void set (String domain, String path, String expires, String name, String value)
    {
        String extra = "";
        if (domain.length() > 0) {
            extra += "; domain=" + domain;
        }
        if (path.length() > 0) {
            extra += "; path=" + path;
        }
        if (expires.length() > 0) {
            extra += "; expires=" + expires;
        }
        doSet(name, value, extra);
    }

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

    /**
     * Handles the actual setting of the cookie.
     */
    protected static native void doSet (String name, String value, String extra) /*-{
        $doc.cookie = "\"" + name + "=" + escape(value) + extra + "\"";
    }-*/;
}
