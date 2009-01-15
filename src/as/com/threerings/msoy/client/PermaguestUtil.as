//
// $Id$

package com.threerings.msoy.client {

/**
 * Methods related to handling permaguests.
 */
public class PermaguestUtil
{
    /** Prefix used when generating email addresses for permaguests. */
    public static const EMAIL_PREFIX :String = "anon";

    /** Suffix used when generating email addresses for permaguests. */
    public static const EMAIL_SUFFIX :String = "@" + DeploymentConfig.serverHost;

    /** Initial name displayed for permaguests. */ 
    public static const DISPLAY_NAME :String = "Guest";

    /** Pattern used to check if an email address is one assigned to a permaguest. */
    public static const EMAIL_PATTERN :RegExp =
        new RegExp(EMAIL_PREFIX + "[0-9a-f]{32}" + EMAIL_SUFFIX, "");

    /**
     * Checks if a user name starts and ends with the right things to be a permaguest email
     * address.
     */
    public static function isPermaguestEmail (address :String) :Boolean
    {
        return address.match(EMAIL_PATTERN) != null;
    }
}
}
