package com.threerings.msoy.web.server;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.samskivert.servlet.util.CookieUtil;

/**
 * Wrapper for a cookie that stores HTTP "Referer" tags on the very first visit to
 * a Whirled page, and gets used later on to auto-populate the ReferralInfo struct.
 * 
 * This class contains server-side functionality; for browser-side code see
 * {@link client.shell.ReferrerCookie}. 
 */
public class ReferrerCookie
{
    /**
     * Is referrer information already stored?
     */
    public static boolean exists (HttpServletRequest req)
    {
        return (CookieUtil.getCookie(req, REFERRAL_FIELD) != null);
    }

    /**
     * Stores a new HTTP referrer string. 
     */
    public static void set (HttpServletResponse rsp, String referrer)
    {
        // TODO: obfuscate the referrer
        
        Cookie cookie = new Cookie(REFERRAL_FIELD, referrer);
        cookie.setMaxAge(365 * 24 * 60 * 60); // leave it there for a year
        cookie.setPath("/");
        rsp.addCookie(cookie);
    }

    private static final String REFERRAL_FIELD = "ref";
}
