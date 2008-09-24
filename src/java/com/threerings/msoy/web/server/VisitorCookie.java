package com.threerings.msoy.web.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.samskivert.servlet.util.CookieUtil;

import com.threerings.msoy.data.all.VisitorInfo;

/**
 * Wrapper for visitor information.
 *
 * <p>
 * Server-side version of {@link client.shell.VisitorCookie}.
 */
public class VisitorCookie
{
    /** Cookie name. */
    public static final String VISITOR = "vis";

    /**
     * Is the visitor information stored anywhere?
     */
    public static boolean exists (HttpServletRequest req)
    {
        return CookieUtil.getCookie(req, VISITOR) != null;
    }

    /**
     * Clears the cookie.
     */
    public static void clear (HttpServletResponse rsp)
    {
        CookieUtil.clearCookie(rsp, VISITOR);
    }

    /**
     * Retrieves visitor information, or null if not found.
     */
    public static VisitorInfo get (HttpServletRequest req)
    {
        if (!exists(req)) {
            return null;
        }

        VisitorInfo info = new VisitorInfo(CookieUtil.getCookieValue(req, VISITOR), false);
        return info;
    }
}
