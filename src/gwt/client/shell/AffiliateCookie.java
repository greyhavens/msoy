//
// $Id$

package client.shell;

import com.threerings.gwt.util.CookieUtil;

import client.util.StringUtil;

import com.threerings.msoy.web.data.TrackingCookieUtil;

/**
 * Client-side of {@link com.threerings.msoy.web.server.AffiliateCookie}.
 */
public class AffiliateCookie
{
    /** {@link com.threerings.msoy.web.server.AffiliateCookie#NAME} */
    public static final String NAME = "baff";

    /**
     * Check to see if we need to store things in a cookie.
     */
    public static void check (String pageToken)
    {
        int idx = pageToken.indexOf(MARKER);
        if (idx == -1) {
            return;
        }

        // see if they already have the cookie. If so, don't overwrite
        if (null != CookieUtil.get(NAME)) {
            return;
        }

        pageToken = pageToken.substring(idx + MARKER.length());
        idx = pageToken.indexOf("_");
        String affiliate = (idx == -1) ? pageToken : pageToken.substring(0, idx);

        // set it, as long as it's not blank
        if (!StringUtil.isBlank(affiliate)) {
            // set the cookie
            String encoded = StringUtil.hexlate(TrackingCookieUtil.encode(affiliate));
            CookieUtil.set("/", 365, NAME, encoded);
        }
    }

    /**
     * Clear the affiliate cookie. It's set on the server!
     */
    public static void clear ()
    {
        CookieUtil.clear("/", NAME);
    }

    protected static final String MARKER = "aid_";
}
