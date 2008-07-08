//
// $Id$

package client.shell;

import com.threerings.gwt.util.CookieUtil;

/**
 * Wrapper that stores and loads up affiliate ids from the browser cookie.
 */
public class AffiliateCookie
{
    /**
     * Add the specified ID to the cookie, overwriting one that was already there.
     */
    public static void put (String aid)
    {
        CookieUtil.set("/", 365, "aid", aid);
        CShell.log("Saved affiliate id [id=" + aid + "].");
    }

    /**
     * Retrieves the affiliate ID, potentially null.
     */
    public static String get ()
    {
        String aid = CookieUtil.get("aid");
        CShell.log("Loaded affiliate id [id=" + aid + "].");
        return aid;
    }
}
    
