//
// $Id$

package com.threerings.msoy.web.server;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.samskivert.servlet.util.CookieUtil;

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
        return CookieUtil.getCookieValue(req, NAME);
    }
}
