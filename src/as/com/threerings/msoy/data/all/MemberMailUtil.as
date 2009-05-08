//
// $Id$

package com.threerings.msoy.data.all {

import com.threerings.msoy.client.DeploymentConfig;

/**
 * Utility routines relating to a member's email address.
 */
public class MemberMailUtil
{
    // declare these before the public constant that uses them to avoid compiler freakout
    protected static const PERMAGUEST_EMAIL_PREFIX :String = "anon";
    protected static const PERMAGUEST_EMAIL_SUFFIX :String = "@" + DeploymentConfig.serverHost;

    /** Regulsr expression used to check if an email address is one assigned to a permaguest. */
    public static const PERMAGUEST_EMAIL_PATTERN :RegExp =
        new RegExp(PERMAGUEST_EMAIL_PREFIX + "[0-9a-f]{32}" + PERMAGUEST_EMAIL_SUFFIX);

    /**
     * Checks if a username (email) matches <code>PERMAGUEST_EMAIL_PATTERN</code>.
     */
    public static function isPermaguest (email :String) :Boolean
    {
        return email.match(PERMAGUEST_EMAIL_PATTERN) != null;
    }
}
}
