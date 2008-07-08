package com.threerings.msoy.bureau.data;

import com.threerings.presents.peer.net.PeerCreds;

/**
 * Credentials for a bureau launcher peer.
 */
public class BureauLauncherCredentials extends PeerCreds
{
    public static final String PREFIX = "bureaulauncher:";

    /** Creates a new bureau launcher credentials for a node. */
    public BureauLauncherCredentials (String nodeName, String sharedSecret)
    {
        super(PREFIX + nodeName, sharedSecret);
    }

    /** Creates a new bureau launcher credentials that will be deserialized into. */
    public BureauLauncherCredentials ()
    {
    }
}
