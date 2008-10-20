//
// $Id$

package client.shell;

import client.util.StringUtil;

import com.threerings.gwt.util.CookieUtil;

import com.threerings.msoy.web.gwt.TrackingCookieUtil;

/**
 * Wrapper that stores and loads up entry vector information. Client-side only.
 */
public class EntryVectorCookie
{
    /** Cookie name. */
    public static final String ENTRY_VECTOR = "ent";

    /** Retrieves visitor information, or null if not found. */
    public static String get ()
    {
        String obfuscated = CookieUtil.get(ENTRY_VECTOR);
        if (obfuscated == null) {
            return null;
        }

        return TrackingCookieUtil.decode(StringUtil.unhexlate(obfuscated));
    }

    /**
     * Saves visitor information in the cookie.
     */
    public static void save (String vector)
    {
        String obfuscated = StringUtil.hexlate(TrackingCookieUtil.encode(vector));
        CookieUtil.set("/", 365, ENTRY_VECTOR, obfuscated);
    }

    /**
     * Completely clears the browser cookie. Used when a player is registered.
     */
    public static void clear ()
    {
        CookieUtil.clear("/", ENTRY_VECTOR);
    }
}
