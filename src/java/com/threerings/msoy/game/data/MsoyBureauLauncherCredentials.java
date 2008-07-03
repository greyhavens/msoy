package com.threerings.msoy.game.data;

import com.threerings.presents.peer.net.PeerCreds;

/**
 * Credentials for a bureau launcher peer.
 */
public class MsoyBureauLauncherCredentials extends PeerCreds
{
    public static final String PREFIX = "bureaulauncher:";

    /** Creates a new bureau launcher credentials for a node. */
    public MsoyBureauLauncherCredentials (String nodeName, String sharedSecret)
    {
        super(PREFIX + nodeName, sharedSecret);
    }

    /** Creates a new bureau launcher credentials that will be deserialized into. */
    public MsoyBureauLauncherCredentials ()
    {
    }
}
