//
// $Id$

package com.threerings.msoy.web.server;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.samskivert.servlet.util.CookieUtil;

import com.threerings.msoy.data.all.VisitorInfo;
import com.threerings.msoy.web.gwt.CookieNames;
import com.threerings.msoy.web.gwt.WebCreds;

/**
 * Wrapper for visitor information.
 *
 * <p>
 * Server-side version of {@link client.shell.VisitorCookie}.
 */
public class VisitorCookie
{
    /**
     * Returns true if we should create a new visitor id for the requester, false if not.
     */
    public static boolean shouldCreate (HttpServletRequest req)
    {
        // don't create if they have a who, visitor or creds cookie
        return (CookieUtil.getCookie(req, CookieNames.WHO) == null &&
                CookieUtil.getCookie(req, CookieNames.VISITOR) == null &&
                CookieUtil.getCookie(req, WebCreds.credsCookie()) == null);
    }

    /**
     * Is the visitor information stored anywhere?
     */
    public static boolean exists (HttpServletRequest req)
    {
        return CookieUtil.getCookie(req, CookieNames.VISITOR) != null;
    }

    /**
     * Clears the cookie.
     */
    public static void clear (HttpServletResponse rsp)
    {
        CookieUtil.clearCookie(rsp, CookieNames.VISITOR);
    }

    /**
     * Retrieves visitor information, or null if not found.
     */
    public static VisitorInfo get (HttpServletRequest req)
    {
        return exists(req) ?
            new VisitorInfo(CookieUtil.getCookieValue(req, CookieNames.VISITOR), false) : null;
    }

    /**
     * Adds the supplied visitor info as a cookie to the supplied response.
     */
    public static void set (HttpServletResponse rsp, VisitorInfo info)
    {
        Cookie cookie = new Cookie(CookieNames.VISITOR, info.id);
        cookie.setMaxAge(365 * 24 * 60 * 60); // 1 year
        cookie.setPath("/");
        rsp.addCookie(cookie);
    }
}
