//
// $Id$

package com.threerings.msoy.web.server;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.samskivert.servlet.util.CookieUtil;

/**
 * Affiliate Cookie Abstraction Jubilee. Bling bling!
 */
public class AffiliateCookie
{
    public static final String NAME = "baff";

    public static void check (HttpServletRequest req, HttpServletResponse rsp)
    {
        if (true) { // TODO: remove
            return;
        }

        String affiliate = req.getParameter("aff"); // "aff" is the parameter name ?aff=foo
        if (affiliate == null) {
            return;
        }

        if (false /* FIRST WINS */ && CookieUtil.getCookie(req, NAME) != null) {
            return;
        }
        // LAST WINS, or FIRST WINS but this is the first!
        Cookie cookie = new Cookie(NAME, affiliate);
        cookie.setMaxAge(365 * 24 * 60 * 60); // one year
        cookie.setPath("/");
        rsp.addCookie(cookie); // nom nom nom
    }

    public static String get (HttpServletRequest req)
    {
        String affiliate = req.getParameter("aff");
        if (affiliate == null) {
            affiliate = CookieUtil.getCookieValue(req, NAME);
        }
        return affiliate;
    }

    public static void clear (HttpServletResponse rsp)
    {
        CookieUtil.clearCookie(rsp, NAME);
    }
}
