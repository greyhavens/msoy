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

/**
 * Wrapper for a cookie that stores HTTP "Referer" tags on the very first visit to
 * a Whirled page, and gets used later on to auto-populate the ReferralInfo struct.
 * 
 * This class contains server-side functionality; for browser-side code see
 * {@link client.shell.HttpReferrerCookie}. 
 */
public class HttpReferrerCookie
{
    /** The name of this cookie. */
    public static final String NAME = "ref";

    /**
     * Check to see if they have the cookie, if not, store it if we can.
     */
    public static void check (HttpServletRequest req, HttpServletResponse rsp)
    {
        if (null != CookieUtil.getCookie(req, NAME)) {
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
        // TODO: obfuscate the referrer?
        
        Cookie cookie = new Cookie(NAME, referrer);
        cookie.setMaxAge(365 * 24 * 60 * 60); // leave it there for a year
        cookie.setPath("/");
        rsp.addCookie(cookie);
    }
}
