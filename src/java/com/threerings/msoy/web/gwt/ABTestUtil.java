//
// $Id$

package com.threerings.msoy.web.gwt;

/**
 * Utilities for executing A/B tests required by the gwt client and the server.
 */
public class ABTestUtil
{
    /**
     * Calculates the test group that a visitor falls into. Assumes the group is enabled and that
     * the user is eligible.
     * @return the group where 1 <= group <= numGroups
     */
    public static int getGroup (String visitorId, String testName, int numGroups)
    {
        // generate the group number based on trackingID + testName
        String hashing = visitorId + testName;

        // this would ideally just use String.hashCode, but that is not cross-platform and we need
        // the number to match on the client and server as well as match old code that used
        // java.lang.String.hashCode
        long seed = 0;
        for (int ii = 0, ll = hashing.length(); ii < ll; ii++) {
            seed = (31 * seed + hashing.charAt(ii)) & 0x0ffffffff;
        }

        return (Math.abs((int)seed) % numGroups) + 1;
    }

    /**
     * When called consecutively for different A/B tests, builds an encoded sequence of tests
     * suitable for use in a cookie to be sent to the web client.
     */
    public static void encodeTest (StringBuilder builder, String testName, int numGroups)
    {
        if (builder.length() > 0) {
            builder.append(COOKIE_RECORD_SEPARATOR);
        }
        builder.append(testName).append(COOKIE_FIELD_SEPARATOR).append(numGroups);
    }

    /**
     * Extracts the number of groups in the test of the given name from the given encoded cookie,
     * or 0 if the name is not in the cookie.
     */
    public static int getNumGroups (String cookie, String testName)
    {
        // this does not need to be more efficient
        for (String item : cookie.split("" + COOKIE_RECORD_SEPARATOR)) {
            String[] parts = item.split("" + COOKIE_FIELD_SEPARATOR);
            if (parts[0].equals(testName)) {
                return Integer.parseInt(parts[1]);
            }
        }
        return 0;
    }

    /** Separates A/B test records in a cookie. */
    protected static final char COOKIE_RECORD_SEPARATOR = ',';

    /** Separates A/B test fields in a cookie. */
    protected static final char COOKIE_FIELD_SEPARATOR = ':';
}
