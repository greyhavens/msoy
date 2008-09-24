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
    /** Cookie name. */
    public static final String VISITOR = "vis";

    /**
     * Is the visitor information stored anywhere?
     */
    public static boolean exists ()
    {
        return CookieUtil.get(VISITOR) != null;
    }

    /**
     * Retrieves visitor information, or null if not found.
     */
    public static VisitorInfo get ()
    {
        if (!exists()) {
            return null;
        }

        VisitorInfo info = new VisitorInfo(CookieUtil.get(VISITOR), false);
        CShell.log("Loaded " + info);
        return info;
    }

    /**
     * Saves visitor information in the cookie, if the cookie doesn't already exist, or if the
     * /overwrite/ flag is set.
     */
    public static void save (VisitorInfo info, boolean overwrite)
    {
        if (!exists() || overwrite) {
            CookieUtil.set("/", 365, VISITOR, info.id);
            CShell.log("Saved " + info);
        }
    }

    /**
     * Completely clears the browser cookie. Used when a player is registered.
     */
    public static void clear ()
    {
        CookieUtil.clear("/", VISITOR);
        CShell.log("Cleared visitor info.");
    }
}
