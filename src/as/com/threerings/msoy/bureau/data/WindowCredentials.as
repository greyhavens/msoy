//
// $Id$

package com.threerings.msoy.bureau.data {

import com.threerings.presents.net.ServiceCreds;

/**
 * Extends the basic credentials to provide window-specific fields.
 */
public class WindowCredentials extends ServiceCreds
{
    /**
     * Creates new credentials for the specific Window.
     */
    public function WindowCredentials (bureauId :String, sharedSecret :String)
    {
        super(bureauId, sharedSecret);
    }
}
}
