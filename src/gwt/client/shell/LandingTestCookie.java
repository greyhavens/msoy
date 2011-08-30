//
// $Id$

package client.shell;

import com.threerings.gwt.util.CookieUtil;

import com.threerings.msoy.web.gwt.ABTestCard;
import com.threerings.msoy.web.gwt.CookieNames;

/**
 * Utility methods related to parsing landing tests given to us by the server in a cookie when we
 * first visit Whirled.
 */
public class LandingTestCookie
{
    /**
     * Gets the landing test of the given name, or null if it is not found.
     */
    public static ABTestCard getTest (String testName)
    {
        return ABTestCard.unflatten(get(), testName);
    }

    /**
     * Gets the value of the landing test cookie, or the empty string if it is not set.
     */
    public static String get ()
    {
        String value = CookieUtil.get(CookieNames.LANDING_TEST);
        return value == null ? "" : value;
    }
}
