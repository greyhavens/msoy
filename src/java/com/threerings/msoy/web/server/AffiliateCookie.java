//
// $Id$

package com.threerings.msoy.web.server;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.samskivert.util.StringUtil;

import com.samskivert.servlet.util.CookieUtil;

import com.threerings.msoy.web.gwt.TrackingCookieUtil;

import static com.threerings.msoy.Log.log;

/**
 * Handles the getting and setting of the affiliate cookie.
 */
public class AffiliateCookie
{
    /** The cookie name. */
    public static final String NAME = "a";

    /**
     * Return the affiliate cookie value.
     */
    public static int get (HttpServletRequest req)
    {
        String aff = TrackingCookieUtil.decode(
            StringUtil.unhexlate(CookieUtil.getCookieValue(req, NAME)));
        try {
            return (aff == null) ? 0 : Integer.parseInt(aff);
        } catch (Exception e) {
            log.info("Rejecting bogus affiliate cookie", "aff", aff);
            return 0;
        }
    }

    /**
     * Stores a new Affiliate cookie with the specified value.
     */
    public static void set (HttpServletResponse rsp, int affiliateId)
    {
        Cookie cookie = new Cookie(
            NAME, StringUtil.hexlate(TrackingCookieUtil.encode(String.valueOf(affiliateId))));
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
