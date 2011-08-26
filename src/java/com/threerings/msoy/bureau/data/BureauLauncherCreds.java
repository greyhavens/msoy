//
// $Id$

package com.threerings.msoy.bureau.data;

import com.threerings.presents.net.ServiceCreds;

/**
 * Provides credentials for a bureau launcher client.
 */
@com.threerings.util.ActionScript(omit=true)
public class BureauLauncherCreds extends ServiceCreds
{
    public BureauLauncherCreds (String nodeName, String sharedSecret)
    {
        super(nodeName, sharedSecret);
    }

    // used when unserializing
    public BureauLauncherCreds ()
    {
    }
}
