//
// $Id$

package com.threerings.msoy.game.server;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link MsoyBureauLauncherService}.
 */
public interface MsoyBureauLauncherProvider extends InvocationProvider
{
    /**
     * Handles a {@link MsoyBureauLauncherService#launcherInitialized} request.
     */
    public void launcherInitialized (ClientObject caller);
}
