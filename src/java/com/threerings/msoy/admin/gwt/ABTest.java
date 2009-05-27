//
// $Id$

package com.threerings.msoy.admin.gwt;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Represents a single a/b test
 */
public class ABTest implements IsSerializable
{
    /** The maximum length for a test name. */
    public static final int MAX_NAME_LENGTH = 50;

    /** The maximum length for a test action. */
    public static final int MAX_ACTION_LENGTH = 32;

    /** A unique id for this test. */
    public int testId;

    /** The unique string identifier for this test. */
    public String name;

    /** Number of equally-sized groups for this test (2 or more) */
    public int numGroups;

    /** Whether to only add visitors to A/B groups if this is their first time on Whirled. */
    public boolean onlyNewVisitors;

    /** True if the test group should be assigned to new users when they land. */
    public boolean landingCookie;

    /** The calculated date on which this test was first started. */
    public Date started;

    /** The calculated date on which this test ended. */
    public Date ended;
}
