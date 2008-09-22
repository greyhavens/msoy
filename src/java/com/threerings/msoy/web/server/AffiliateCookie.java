//
// $Id$

package com.threerings.msoy.web.server;

import javax.servlet.http.HttpServletRequest;

import com.samskivert.util.StringUtil;

import com.samskivert.servlet.util.CookieUtil;

import com.threerings.msoy.web.data.TrackingCookieUtil;

/**
 * The server-side of {@link client.shell.AffiliateCookie}.
 */
public class AffiliateCookie
{
    /** The cookie name. */
    public static final String NAME = "baff";

    /**
     * Return the affiliate cookie value.
     */
    public static String get (HttpServletRequest req)
    {
        String cook = CookieUtil.getCookieValue(req, NAME);
        if (cook != null) {
            cook = TrackingCookieUtil.decode(StringUtil.unhexlate(cook));
        }
        return cook;
    }
}
