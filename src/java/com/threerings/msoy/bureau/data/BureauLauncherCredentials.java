//
// $Id$

package com.threerings.msoy.bureau.data;

import com.samskivert.util.StringUtil;
import com.threerings.presents.net.UsernamePasswordCreds;
import com.threerings.util.Name;

/**
 * Credentials for a bureau launcher client.
 */
public class BureauLauncherCredentials extends UsernamePasswordCreds
{
    public static final String PREFIX = "bureaulauncher:";

    public static String createPassword (String nodeName, String sharedSecret)
    {
        return StringUtil.md5hex(nodeName + sharedSecret);
    }

    /** Creates a new bureau launcher credentials for a node. */
    public BureauLauncherCredentials (String nodeName, String sharedSecret)
    {
        super(new Name(PREFIX + nodeName), createPassword(nodeName, sharedSecret));
    }

    /** Creates a new bureau launcher credentials that will be deserialized into. */
    public BureauLauncherCredentials ()
    {
    }

    public String getNodeName ()
    {
        return getUsername().toString().substring(PREFIX.length());
    }
}
