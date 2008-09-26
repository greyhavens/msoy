//
// $Id$

package com.threerings.msoy.web.server;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.samskivert.util.StringUtil;

import com.samskivert.servlet.util.CookieUtil;

import com.threerings.msoy.web.data.TrackingCookieUtil;

/**
 * The server-side of {@link client.shell.AffiliateCookie}.
 */
public class AffiliateCookie
{
    /** The cookie name. */
    public static final String NAME = "aff2";

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

    /**
     * Stores a new Affiliate cookie with the specified value.
     */
    public static void set (HttpServletResponse rsp, String affiliate)
    {
        Cookie cookie = new Cookie(NAME,
            StringUtil.hexlate(TrackingCookieUtil.encode(affiliate.trim())));
        cookie.setMaxAge(365 * 24 * 60 * 60); // 1 year
        cookie.setPath("/");
        rsp.addCookie(cookie);
    }

    /**
     * Clear the cookie.
     */
    public static void clear (HttpServletResponse rsp)
    {
        CookieUtil.clearCookie(rsp, NAME);
    }
}
