//
// $Id: Audio.java 9514 2008-06-19 23:06:16Z nathan $

package com.threerings.msoy.admin.gwt;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Represents a single a/b test
 */
public class ABTest implements IsSerializable
{
    /** Id. */
    public int abTestId;

    /** The unique string identifier for this test, used to reference it when switching content. */
    public String name;

    /** More detailed description of this test. */
    public String description;

    /** Number of equally-sized groups for this test (2 or more) */
    public int numGroups;

    /** Only add visitors to a/b groups if this is their first time on whirled */
    public boolean onlyNewVisitors;

    /** Only add visitors to a/b groups if they come from this affiliate */
    public String affiliate;

    /** Only add visitors to a/b groups if they come from this vector */
    public String vector;

    /** Only add visitors to a/b groups if they come from this creative */
    public String creative;

    /** Is this test being run on the site right now? */
    public boolean enabled;

    /** The calculated date on which this test was last enabled. */
    public Date started;

    /** The calculated date on which this test was last disabled. */
    public Date ended;

    /** The maximum length for Name strings. */
    public static final int MAX_NAME_LENGTH = 50;
    public static final int MAX_DESCRIPTION_LENGTH = 200;
    public static final int MAX_AFFILIATE_LENGTH = 50;
    public static final int MAX_VECTOR_LENGTH = 50;
    public static final int MAX_CREATIVE_LENGTH = 50;

    /**
     * Constructor; set a few defaults
     */
    public ABTest ()
    {
        onlyNewVisitors = true;
        numGroups = 2;
    }

    /**
     * Throw an error if there is a problem with any of the test data.
     */
    public void validate ()
        throws Exception
    {
        if (name == null || name.length() == 0) {
            throw new Exception("No name entered.");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new Exception("Name can be " + MAX_NAME_LENGTH + " characters max");
        }
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new Exception("Description can be " + MAX_DESCRIPTION_LENGTH + " characters max");
        }
    }
}
