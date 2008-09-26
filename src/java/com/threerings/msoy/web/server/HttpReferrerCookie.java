//
// $Id$

package com.threerings.msoy.web.server;

import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.samskivert.util.StringUtil;

import com.samskivert.servlet.util.CookieUtil;
import com.threerings.msoy.web.data.TrackingCookieUtil;

/**
 * Wrapper for a cookie that stores HTTP "Referer" tags on the very first visit to
 * a Whirled page, and gets logged later to Panopticon.
 *
 * This class contains server-side functionality; for browser-side code see
 * {@link client.shell.HttpReferrerCookie}.
 */
public class HttpReferrerCookie
{
    /** The name of this cookie. */
    public static final String NAME = "vre";

    /**
     * Check to see if they have the cookie, if not, store it if we can.
     */
    public static void check (HttpServletRequest req, HttpServletResponse rsp)
    {
        if (CookieUtil.getCookie(req, NAME) != null) {
            return; // we already got one!
        }

        String ref = req.getHeader("Referer");
        if (!StringUtil.isBlank(ref)) {
            try {
                set(rsp, new URL(ref).getHost());
            } catch (MalformedURLException mue) {
                // don't create the cookie..
            }
        }
    }

    /**
     * Stores a new HTTP referrer string.
     */
    public static void set (HttpServletResponse rsp, String referrer)
    {
        // obfuscate the referrer
        String obfuscated = StringUtil.hexlate(TrackingCookieUtil.encode(referrer));

        Cookie cookie = new Cookie(NAME, obfuscated);
        cookie.setMaxAge(365 * 24 * 60 * 60); // leave it there for a year
        cookie.setPath("/");
        rsp.addCookie(cookie);
    }
}
