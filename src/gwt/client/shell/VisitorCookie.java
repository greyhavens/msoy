//
// $Id$

package client.shell;

import com.threerings.gwt.util.CookieUtil;

import com.threerings.msoy.data.all.VisitorInfo;
import com.threerings.msoy.web.gwt.CookieNames;

/**
 * Wrapper that stores and loads up visitor information.
 *
 * <p> Client-side version of {@link com.threerings.msoy.web.server.VisitorCookie}.
 */
public class VisitorCookie
{
    /**
     * Is the visitor information stored anywhere?
     */
    public static boolean exists ()
    {
        return CookieUtil.get(CookieNames.VISITOR) != null;
    }

    /**
     * Retrieves visitor information, or null if not found.
     */
    public static VisitorInfo get ()
    {
        if (!exists()) {
            return null;
        }

        VisitorInfo info = new VisitorInfo(CookieUtil.get(CookieNames.VISITOR), false);
        CShell.log("Loaded " + info);
        return info;
    }

    /**
     * When a player logs on and we receive their canonical visitorId from the server, we want
     * to use the new one everywhere in the client as well as set the cookie to the new info.
     */
    public static VisitorInfo upgrade (VisitorInfo authoritative)
    {
        if (!authoritative.isAuthoritative) {
            CShell.log("Huh? Asked to upgrade to a non-authoritative visitor info.");
            return get();
        }
        String id = CookieUtil.get(CookieNames.VISITOR);
        // if the authoritative cookie is different from what we have (or we have nothing), replace
        if (id == null || !id.equals(authoritative.id)) {
            CookieUtil.set("/", 365, CookieNames.VISITOR, authoritative.id, null);
            CShell.log("Updated to " + authoritative);
            id = authoritative.id;
        }
        return new VisitorInfo(id, true);
    }

    /**
     * Saves visitor information in the cookie, if the cookie doesn't already exist, or if the
     * /overwrite/ flag is set.
     */
    public static void save (VisitorInfo info, boolean overwrite)
    {
        if (!exists() || overwrite) {
            CookieUtil.set("/", 365, CookieNames.VISITOR, info.id, null);
            CShell.log("Saved " + info);
        }
    }

    /**
     * Completely clears the browser cookie. Used when a player is registered.
     */
    public static void clear ()
    {
        CookieUtil.clear("/", CookieNames.VISITOR);
        CShell.log("Cleared visitor info.");
    }
}
