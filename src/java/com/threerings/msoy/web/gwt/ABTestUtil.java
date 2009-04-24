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
        int seed = Math.abs((visitorId + testName).hashCode());
        return (seed % numGroups) + 1;
    }
}
