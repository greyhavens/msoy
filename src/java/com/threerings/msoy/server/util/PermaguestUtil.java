//
// $Id$

package com.threerings.msoy.server.util;

import java.security.MessageDigest;
import java.util.regex.Pattern;

import com.samskivert.util.StringUtil;
import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.server.ServerConfig;

/**
 * Code related to permaguests. May be moved later.
 */
public class PermaguestUtil
{
    /** Whether we create accounts for guests. */
    public static final boolean ENABLED = DeploymentConfig.devDeployment &&
        "true".equals(System.getProperty("permaguests", null));

    /** Prefix used when generating email addresses for permaguests. */
    public static final String EMAIL_PREFIX = "anon";

    /** Suffix used when generating email addresses for permaguests. */
    public static final String EMAIL_SUFFIX = "@" + ServerConfig.serverHost;

    /** Pattern used to check if an email address is one assigned to a permaguest. */
    public static final Pattern EMAIL_PATTERN =
        Pattern.compile(EMAIL_PREFIX + "[0-9a-f]{32}" + EMAIL_SUFFIX);

    /**
     * Checks if a user name starts and ends with the right things to be a permaguest email
     * address.
     */
    public static boolean isPermaguestEmail (String address)
    {
        return EMAIL_PATTERN.matcher(address).matches();
    }

    public static String generateDisplayName (int memberId)
    {
        return DISPLAY_PREFIX + " " + memberId;
    }

    /**
     * Creates a username to give permanence to unregistered users.
     */
    public static String createUsername (String ipAddress)
    {
        // generate some unique stuff
        String hashSource = "" + System.currentTimeMillis() + ":" + ipAddress + ":" + Math.random();

        // hash it
        byte[] digest;
        try {
            digest = MessageDigest.getInstance("MD5").digest(hashSource.getBytes());

        } catch (java.security.NoSuchAlgorithmException nsae) {
            throw new RuntimeException("MD5 not found!?");
        }

        if (digest.length != 16) {
            throw new RuntimeException("Odd MD5 digest: " + StringUtil.hexlate(digest));
        }

        // convert to an email address
        return EMAIL_PREFIX + StringUtil.hexlate(digest) + EMAIL_SUFFIX;
    }

    /** Prefix of permaguest display names. They have to create an account to get a real one. */ 
    protected static final String DISPLAY_PREFIX = "Guest";
}
