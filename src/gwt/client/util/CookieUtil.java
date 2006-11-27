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
     *
     * @param expires the number of days in which the cookie should expire.
     */
    public static void set (String domain, String path, int expires, String name, String value)
    {
        String extra = "";
        if (domain.length() > 0) {
            extra += "; domain=" + domain;
        }
        if (path.length() > 0) {
            extra += "; path=" + path;
        }
        doSet(name, value, expires, extra);
    }

    /**
     * Clears out the specified cookie.
     */
    public static void clear (String domain, String path, String name)
    {
        set(domain, path, -1, name, "");
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
    protected static native void doSet (String name, String value, int expires, String extra) /*-{
        if (expires != 0) {
            var date = new Date();
            date.setTime(date.getTime() + (expires*24*60*60*1000));
            extra += "; expires=" + date.toGMTString();
        }
        $doc.cookie = name + "=" + escape(value) + extra;
    }-*/;
}
