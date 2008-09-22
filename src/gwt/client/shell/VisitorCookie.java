//
// $Id$

package client.shell;

import com.threerings.gwt.util.CookieUtil;

import com.threerings.msoy.data.all.VisitorInfo;

/**
 * Wrapper that stores and loads up visitor information.
 *
 * <p>
 * Client-side version of {@link com.threerings.msoy.web.server.VisitorCookie}.
 */
public class VisitorCookie
{
    /**
     * Is the visitor information stored anywhere?
     */
    public static boolean exists ()
    {
        return CookieUtil.get(VISITOR_ID) != null;
    }

    /**
     * Retrieves visitor information, or null if not found.
     */
    public static VisitorInfo get ()
    {
        if (!exists()) {
            return null;
        }

        VisitorInfo info = new VisitorInfo(CookieUtil.get(VISITOR_ID));
        CShell.log("Loaded " + info.toString());
        return info;
    }

    /**
     * Saves visitor information in the cookie, if the cookie doesn't already exist, or if the
     * /overwrite/ flag is set.
     */
    public static void save (VisitorInfo info, boolean overwrite)
    {
        if (!exists() || overwrite) {
            CookieUtil.set("/", 365, VISITOR_ID, info.tracker);
            CShell.log("Saved " + info);
        }
    }

    /**
     * Completely clears the browser cookie. Used when a player is registered.
     */
    public static void clear ()
    {
        CookieUtil.clear("/", VISITOR_ID);
        CShell.log("Cleared referral info.");
    }

    /** Cookie name. */
    private static final String VISITOR_ID = "vis";
}
