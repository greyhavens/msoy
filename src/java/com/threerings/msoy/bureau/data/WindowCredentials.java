//
// $Id$

package com.threerings.msoy.bureau.data;

import com.threerings.presents.net.ServiceCreds;

/**
 * Extends the basic credentials to provide window-specific fields.
 */
public class WindowCredentials extends ServiceCreds
{
    /**
     * Creates an empty credentials for streaming. Should not be used directly.
     */
    public WindowCredentials ()
    {
    }

    /**
     * Creates new credentials for a specific bureau.
     */
    public WindowCredentials (String bureauId, String sharedSecret)
    {
        super(bureauId, sharedSecret);
    }
}
