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
}
