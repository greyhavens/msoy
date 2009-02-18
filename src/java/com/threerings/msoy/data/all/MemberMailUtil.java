//
// $Id$

package com.threerings.msoy.data.all;

/**
 * Utility routines relating to a member's email address.
 */
public class MemberMailUtil
{
    // these have to be declared before the public constant that uses them below; lame-ass compiler
    protected static final String PERMAGUEST_EMAIL_PREFIX = "anon";
    protected static final String PERMAGUEST_EMAIL_SUFFIX = "@" + DeploymentConfig.serverHost;

    /** Regular expression used to check if an email address is one assigned to a permaguest. */
    public static final String PERMAGUEST_EMAIL_PATTERN =
        PERMAGUEST_EMAIL_PREFIX + "[0-9a-f]{32}" + PERMAGUEST_EMAIL_SUFFIX;

    /** Regular expressions that match our various placeholder addresses. */
    public static final String[] PLACEHOLDER_PATTERNS = {
        "[0-9]+@facebook.com",
        PERMAGUEST_EMAIL_PATTERN,
    };

    /**
     * Returns true if the supplied email address is a placeholder, false otherwise. Placeholder
     * addresses are used when we have no email address from the user because their account was
     * auto-created (using an external authentication source like Facebook, or because they are a
     * permaguest).
     */
    public static boolean isPlaceholderAddress (String email)
    {
        for (String pattern : PLACEHOLDER_PATTERNS) {
            if (pattern.matches(email)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if an account name (email) matches {@link #PERMAGUEST_EMAIL_PATTERN}.
     */
    public static boolean isPermaguest (String email)
    {
        return email.matches(PERMAGUEST_EMAIL_PATTERN);
    }

    /**
     * Creates a permaguest email address given the supplied hex encoded hash blob (which must
     * match the following regex: [0-9a-f]{32}).
     */
    public static String makePermaguestEmail (String hash)
    {
        return PERMAGUEST_EMAIL_PREFIX + hash + PERMAGUEST_EMAIL_SUFFIX;
    }
}
