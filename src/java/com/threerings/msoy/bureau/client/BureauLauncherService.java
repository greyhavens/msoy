//
// $Id$

package com.threerings.msoy.bureau.client;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.Client;

/**
 * Service for use by a bureau launcher.
 */
public interface BureauLauncherService extends InvocationService
{
    /**
     * Lets the server know that this launcher is ready to go.
     */
    void launcherInitialized (Client caller);
}
