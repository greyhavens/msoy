//
// $Id$

package com.threerings.msoy.web.gwt;

import java.util.Date;

import com.threerings.msoy.data.all.VisitorInfo;

/**
 * Contains just enough information to calculate a user's test group on the client.
 */
public class ABTestCard
{
    /** The name of the test. */
    public String name;

    /** When the test was started. */
    public Date started;

    /** The number of groups in the test. */
    public int numGroups;

    /** If true, previously cookied visitors are not eligible. */
    public boolean onlyNewVisitors;

    /**
     * Extracts the test card from a previously flattened version of a list of tests. Returns null
     * if no test of the given name is found.
     */
    public static ABTestCard unflatten (String cookie, String testName)
    {
        // the browser does not unquote the cookie value... but then again, maybe it will so just
        // get rid of all quotes; we don't need them
        cookie = cookie.replace("\"", "");

        // this does not need to be more efficient
        for (String item : cookie.split(String.valueOf(RECORD_SEP))) {
            String[] parts = item.split(String.valueOf(FIELD_SEP));
            if (parts[0].equals(testName)) {
                int numGroups = Integer.parseInt(parts[1]);
                long started = parts.length > 2 ? Long.parseLong(parts[2]) : 0;
                char eligibility = parts.length > 3 ? parts[3].charAt(0) : ALL_USERS;
                return new ABTestCard(
                    testName, new Date(started), numGroups, eligibility == NEW_USERS_ONLY);
            }
        }
        return null;
    }

    /**
     * Creates a new test card.
     */
    public ABTestCard (String testName, Date started, int numGroups, boolean onlyNewVisitors)
    {
        this.name = testName;
        this.started = started;
        this.numGroups = numGroups;
        this.onlyNewVisitors = onlyNewVisitors;
    }

    /**
     * Flattens the test card into a buffer. Separates tests if called multiple times.
     */
    public void flatten (StringBuilder builder)
    {
        if (builder.length() > 0) {
            builder.append(RECORD_SEP);
        }
        builder.append(name);
        builder.append(FIELD_SEP).append(numGroups);
        builder.append(FIELD_SEP).append(started.getTime());
        builder.append(FIELD_SEP).append(onlyNewVisitors ? NEW_USERS_ONLY : ALL_USERS);
    }

    /**
     * Returns the test group assigned to a visitor with the given id, or -1 if not eligible.
     * @return the group where 1 <= group <= numGroups
     */
    public int getGroup (VisitorInfo info)
    {
        // test runs only on new users and visitor is returning
        // (visitor may have been in a group during a previous session!)
        if (onlyNewVisitors && started.after(info.getCreationTime())) {
            return -1;
        }

        // generate the group number based on trackingID + testName
        String hashing = info.id + name;

        // this would ideally just use String.hashCode, but that is not cross-platform and we need
        // the number to match on the client and server as well as match old code that used
        // java.lang.String.hashCode
        long seed = 0;
        for (int ii = 0, ll = hashing.length(); ii < ll; ii++) {
            seed = (31 * seed + hashing.charAt(ii)) & 0x0ffffffff;
        }

        return (Math.abs((int)seed) % numGroups) + 1;
    }

    /** Separates A/B test records in a cookie. */
    protected static final char RECORD_SEP = ',';

    /** Separates A/B test fields in a cookie. */
    protected static final char FIELD_SEP = ':';

    /** Indicates new users only. */
    protected static final char NEW_USERS_ONLY = 'n';

    /** Indicates all users. */
    protected static final char ALL_USERS = 'a';
}
