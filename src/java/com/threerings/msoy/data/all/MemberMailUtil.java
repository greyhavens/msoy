//
// $Id$

package com.threerings.msoy.data.all;

/**
 * Utility routines relating to a member's email address.
 */
public class MemberMailUtil
{
    /** Regular expressions that match our various placeholder addresses. */
    public static final String[] PLACEHOLDER_PATTERNS;

    /**
     * Returns true if the supplied email address is a placeholder, false otherwise. Placeholder
     * addresses are used when we have no email address from the user because their account was
     * auto-created (using an external authentication source like Facebook, or because they are a
     * permaguest).
     */
    public static boolean isPlaceholderAddress (String email)
    {
        for (String pattern : PLACEHOLDER_PATTERNS) {
            if (email.matches(pattern)) {
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
     * Extracts a previously embedded dongle from the given permaguest email address. Returns 0
     * if none is attached or if the email is not a permaguest one.
     */
    public static char extractPermaguestDongle (String email)
    {
        if (!isPermaguest(email) || email.length() != DONGLED_EMAIL_LENGTH) {
            return 0;
        }
        return email.charAt(PERMAGUEST_EMAIL_PREFIX.length() + HASH_LENGTH);
    }

    /**
     * Creates a permaguest email address given the supplied hex encoded hash blob (which must
     * match the following regex: [0-9a-f]{32}). A dongle character may be attached that can later
     * be checked when the permaguest registers. The dongle must not be a hex digit. If zero, it is
     * not attached.
     */
    public static String makePermaguestEmail (String hash, char dongle)
    {
        String dongleBit = dongle > 0 ? String.valueOf(dongle) : "";
        return PERMAGUEST_EMAIL_PREFIX + hash + dongleBit + PERMAGUEST_EMAIL_SUFFIX;
    }

    /**
     * Constructs a low-tech pattern for detecting permaguest emails using only a wildcard
     * expression. The pattern may match more than the range of values returned by
     * {@link #makePermaguestEmail} since a wilcard will not measure length or content.
     * @param wildcard equivalent of ".*" regular expression
     */
    public static String makePermaguestPattern(String wildcard)
    {
        return PERMAGUEST_EMAIL_PREFIX + wildcard + PERMAGUEST_EMAIL_SUFFIX;
    }

    // these have to be declared before the public constant that uses them below; lame-ass compiler
    protected static final String PERMAGUEST_EMAIL_PREFIX = "anon";
    protected static final String PERMAGUEST_EMAIL_SUFFIX = "@" + DeploymentConfig.serverHost;
    protected static final int HASH_LENGTH = 32;
    protected static final String HASH_PATTERN = "[0-9a-f]{" + HASH_LENGTH + "}";
    protected static final String DONGLE_PATTERN = ".?";
    protected static final int DONGLED_EMAIL_LENGTH = 
        PERMAGUEST_EMAIL_PREFIX.length() + HASH_LENGTH + 1 + PERMAGUEST_EMAIL_SUFFIX.length();

    /** Regular expression used to check if an email address is one assigned to a permaguest. */
    protected static final String PERMAGUEST_EMAIL_PATTERN =
        PERMAGUEST_EMAIL_PREFIX + HASH_PATTERN + DONGLE_PATTERN + PERMAGUEST_EMAIL_SUFFIX;

    static
    {
        PLACEHOLDER_PATTERNS = new String[] {
            "[0-9]+@facebook.com",
            PERMAGUEST_EMAIL_PATTERN,
        };
    }
}
