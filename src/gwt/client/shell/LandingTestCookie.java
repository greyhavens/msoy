package client.shell;

import com.threerings.gwt.util.CookieUtil;
import com.threerings.msoy.web.gwt.ABTestUtil;

/**
 * Utility methods related to parsing landing tests given to us by the server in a cookie when we
 * first visit Whirled.
 */
public class LandingTestCookie
{
    /** The name of the landing test cookie. */
    public static final String NAME = "lt";

    /**
     * Gets the group assigned to the given visitor id for the given test name. Note that this
     * should only be called by new users, i.e. when the landing page is being accessed and there
     * is no previous member cookie.
     * @return the group (>=1) or -1 if the test is not active
     */
    public static int getGroup (String testName, String visitorId)
    {
        int numGroups = getNumGroups(testName);
        return numGroups == 0 ? -1 : ABTestUtil.getGroup(visitorId, testName, numGroups);
    }

    /**
     * Gets the number of groups in the test of the given name, or 0 if the server did not tell us
     * about the test.
     */
    public static int getNumGroups (String testName)
    {
        for (String item : get().split(";")) {
            String[] parts = item.split(":");
            if (parts[0].equals(testName)) {
                return Integer.parseInt(parts[1]);
            }
        }
        return 0;
    }

    /**
     * Gets the value of the landing test cookie, or the empty string if it is not set.
     */
    public static String get ()
    {
        String value = CookieUtil.get(NAME);
        return value == null ? "" : value;
    }
}
