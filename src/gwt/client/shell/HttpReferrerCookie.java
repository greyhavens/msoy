//
// $Id$

package client.shell;

import client.util.StringUtil;

import com.threerings.gwt.util.CookieUtil;
import com.threerings.msoy.web.data.TrackingCookieUtil;

/**
 * Wrapper for a cookie that stores HTTP "Referer" tags on the very first visit to
 * a Whirled page, and gets logged lated to Panopticon.
 *
 * This class contains browser-side functionality; for server-side code see
 * {@link com.threerings.msoy.web.server.HttpReferrerCookie}.
 */
public class HttpReferrerCookie
{
    public static final String NAME = "ref";

    /**
     * Returns true if a valid, not-disabled referrer information is available in a cookie.
     */
    public static boolean available ()
    {
        String raw = CookieUtil.get(NAME);
        return (raw != null) && !REFERRER_DISABLED_VALUE.equals(raw);
    }

    /**
     * Retrieves saved HTTP referrer. Returns null if one has not been saved.
     */
    public static String get ()
    {
        String raw = CookieUtil.get(NAME);
        if (raw == null) {
            return null;
        }

        byte[] data = StringUtil.unhexlate(raw);
        if (data == null) {
            CShell.log("Dropping malformed referrer " + raw + ".");
            return null;
        }

        String ref = TrackingCookieUtil.decode(data);
        CShell.log("Loaded referrer: " + ref);
        return ref;
    }

    /**
     * Marks HTTP referrer as "disabled". This will prevent the server from trying
     * to overwrite it on future page views.
     */
    public static void disable ()
    {
        CookieUtil.set("/", 365, NAME, REFERRER_DISABLED_VALUE);
        CShell.log("Referrer disabled.");
    }

    /**
     * Some value that's not null (so the existence check passes), but
     * also does not contain a meaningful HTTP Reference string.
     */
    private static final String REFERRER_DISABLED_VALUE = "";
}

