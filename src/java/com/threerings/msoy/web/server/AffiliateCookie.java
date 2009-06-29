//
// $Id$

package com.threerings.msoy.web.server;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.samskivert.servlet.util.CookieUtil;
import com.threerings.msoy.web.gwt.CookieNames;

import static com.threerings.msoy.Log.log;

/**
 * Contains affiliate information for new users and static methods for getting and setting the
 * cookie.
 */
public class AffiliateCookie
{
    /** The id of the member who will become the new user's affiliate. */
    public int memberId;

    /** If a friend request should be automatically sent to the affiliate on registration. */
    public boolean autoFriend;

    /**
     * Return the cookie from an http request. If there is no affiliate cookie, the cgi parameter
     * of the same name is also checked. 
     */
    public static AffiliateCookie fromWeb (HttpServletRequest req)
    {
        String affCookie = CookieUtil.getCookieValue(req, CookieNames.AFFILIATE);
        String aff = (affCookie == null) ? req.getParameter(CookieNames.AFFILIATE) : affCookie;
        int id = 0;
        try {
            id = (aff == null) ? 0 : Integer.parseInt(aff);
        } catch (Exception e) {
            log.info("Rejecting bogus affiliate", "cookie", affCookie, "aff", aff);
        }
        return fromCreds(id);
    }

    /**
     * Return the cookie from an id given to us via a world login.
     */
    public static AffiliateCookie fromCreds (int id)
    {
        AffiliateCookie cook = new AffiliateCookie();
        cook.autoFriend = id < 0;
        cook.memberId = Math.abs(id);
        return cook;
    }

    /**
     * Stores a new cookie in an http response with the specified value and whether or not a friend
     * request should be automatically initiated if the user eventually registers.
     */
    public static void set (HttpServletResponse rsp, int affiliateId, boolean autoFriendReq)
    {
        if (autoFriendReq) {
            affiliateId = -affiliateId;
        }
        Cookie cookie = new Cookie(CookieNames.AFFILIATE, String.valueOf(affiliateId));
        cookie.setMaxAge(365 * 24 * 60 * 60); // 1 year
        cookie.setPath("/");
        rsp.addCookie(cookie);
    }

    /**
     * Clear the cookie.
     */
    public static void clear (HttpServletResponse rsp)
    {
        CookieUtil.clearCookie(rsp, CookieNames.AFFILIATE);
    }
}
